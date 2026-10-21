import {
  cancel,
  confirm,
  intro,
  isCancel,
  multiselect,
  note,
  outro,
  select,
  spinner,
  text,
} from "@clack/prompts";
import { appendFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { spawn, spawnSync } from "node:child_process";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const isWindows = process.platform === "win32";
const gradleCommand = join(root, isWindows ? "gradlew.bat" : "gradlew");
const semverPattern = /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/;
let activeLogDirectory = null;
let activeReleaseLog = null;

function timestamp() {
  return new Date().toISOString();
}

function fileTimestamp() {
  return timestamp().replaceAll(":", "-").replaceAll(".", "-");
}

function slug(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/gu, "-").replace(/^-|-$/gu, "");
}

function appendReleaseLog(message) {
  if (activeReleaseLog) appendFileSync(activeReleaseLog, message, "utf8");
}

function initializeReleaseLog(details) {
  activeLogDirectory = join(root, ".logs", "releases", `${fileTimestamp()}_v${details.version}`);
  mkdirSync(activeLogDirectory, { recursive: true });
  activeReleaseLog = join(activeLogDirectory, "release.log");
  const metadata = {
    startedAt: timestamp(),
    ...details,
    cwd: root,
    platform: `${process.platform} ${process.arch}`,
    node: process.version,
    javaHome: details.env.JAVA_HOME || null,
    credentialsPresent: {
      modrinth: Boolean(details.env.MODRINTH_TOKEN),
      curseforge: Boolean(details.env.CURSEFORGE_TOKEN),
      github: Boolean(details.env.GH_TOKEN || details.env.GITHUB_TOKEN),
    },
  };
  delete metadata.env;
  writeFileSync(join(activeLogDirectory, "release.json"), `${JSON.stringify(metadata, null, 2)}\n`, "utf8");
  writeFileSync(activeReleaseLog, `[${metadata.startedAt}] Release started\n${JSON.stringify(metadata, null, 2)}\n\n`, "utf8");
}

function stop(message = "Release cancelled.") {
  cancel(message);
  process.exit(0);
}

function unwrap(value) {
  if (isCancel(value)) stop();
  return value;
}

function readProperties(path) {
  const values = new Map();
  for (const rawLine of readFileSync(path, "utf8").split(/\r?\n/u)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const separator = line.indexOf("=");
    if (separator > 0) {
      values.set(line.slice(0, separator).trim(), line.slice(separator + 1).trim());
    }
  }
  return values;
}

function readDotEnv(path) {
  if (!existsSync(path)) return new Map();
  const values = readProperties(path);
  for (const [key, value] of values) {
    values.set(key, value.replace(/^(['"])(.*)\1$/u, "$2"));
  }
  return values;
}

function bumpVersion(current, kind) {
  const match = current.match(/^(\d+)\.(\d+)\.(\d+)(.*)$/u);
  if (!match) throw new Error(`Cannot bump invalid mod_version '${current}'.`);
  let major = Number(match[1]);
  let minor = Number(match[2]);
  let patch = Number(match[3]);
  if (kind === "major") {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (kind === "minor") {
    minor += 1;
    patch = 0;
  } else {
    patch += 1;
  }
  return `${major}.${minor}.${patch}${match[4]}`;
}

function javaMajor(javaHome) {
  if (!javaHome) return 0;
  const executable = join(javaHome, "bin", isWindows ? "java.exe" : "java");
  if (!existsSync(executable)) return 0;
  const result = spawnSync(executable, ["-version"], { encoding: "utf8" });
  const output = `${result.stdout || ""}\n${result.stderr || ""}`;
  const match = output.match(/version\s+"(?:1\.)?(\d+)/u);
  return match ? Number(match[1]) : 0;
}

function resolveJavaHome() {
  const candidates = [process.env.JAVA_HOME];
  if (isWindows) {
    try {
      const settings = JSON.parse(readFileSync(join(root, ".vscode", "settings.json"), "utf8"));
      candidates.push(settings["java.import.gradle.java.home"]);
      for (const runtime of settings["java.configuration.runtimes"] || []) {
        candidates.push(runtime.path);
      }
    } catch {
      // VS Code settings are optional.
    }
    const home = process.env.USERPROFILE;
    if (home) {
      candidates.push(join(home, ".jdks", "jdk-25"), join(home, ".jdks", "jdk-21"));
    }
  }
  return candidates.find((candidate) => javaMajor(candidate) >= 17) || null;
}

function releaseEnvironment() {
  const env = { ...process.env };
  for (const [key, value] of readDotEnv(join(root, ".env"))) {
    if (!env[key]) env[key] = value;
  }
  const javaHome = resolveJavaHome();
  if (javaHome) {
    env.JAVA_HOME = javaHome;
    env.PATH = `${join(javaHome, "bin")}${isWindows ? ";" : ":"}${env.PATH || ""}`;
  }
  return env;
}

async function run(command, args, label, env, dryRun = false) {
  const rendered = [command, ...args].join(" ");
  const startedAt = timestamp();
  const started = Date.now();
  const taskLog = activeLogDirectory
    ? join(activeLogDirectory, `${String(run.sequence++).padStart(2, "0")}-${slug(label)}.log`)
    : null;
  const header = [
    `[${startedAt}] ${label}`,
    `Command: ${rendered}`,
    `Working directory: ${root}`,
    `JAVA_HOME: ${env.JAVA_HOME || "<not set>"}`,
    `Mode: ${dryRun ? "dry-run" : "execute"}`,
    "",
  ].join("\n");
  if (taskLog) writeFileSync(taskLog, header, "utf8");
  appendReleaseLog(header);
  if (dryRun) {
    note(rendered, label);
    const ending = `[${timestamp()}] SKIPPED (dry run)\n\n`;
    if (taskLog) appendFileSync(taskLog, ending, "utf8");
    appendReleaseLog(ending);
    return;
  }
  const progress = spinner();
  progress.start(label);
  const useWindowsShell = isWindows && command.toLowerCase().endsWith(".bat");
  const shellCommand = [command, ...args]
    .map((arg) => `"${String(arg).replaceAll('"', '""')}"`)
    .join(" ");
  const result = await new Promise((resolveRun) => {
    const child = spawn(useWindowsShell ? shellCommand : command, useWindowsShell ? [] : args, {
      cwd: root,
      env,
      stdio: ["inherit", "pipe", "pipe"],
      shell: useWindowsShell,
    });
    let spawnError = null;
    const capture = (stream, terminal) => stream.on("data", (chunk) => {
      terminal.write(chunk);
      if (taskLog) appendFileSync(taskLog, chunk);
      appendReleaseLog(chunk);
    });
    capture(child.stdout, process.stdout);
    capture(child.stderr, process.stderr);
    child.on("error", (error) => { spawnError = error; });
    child.on("close", (status, signal) => resolveRun({ status, signal, error: spawnError }));
  });
  const durationMs = Date.now() - started;
  const ending = `\n[${timestamp()}] Exit code: ${result.status ?? "none"}; signal: ${result.signal || "none"}; duration: ${durationMs} ms\n\n`;
  if (taskLog) appendFileSync(taskLog, ending, "utf8");
  appendReleaseLog(ending);
  if (result.error || result.status !== 0) {
    progress.stop(`${label} failed`);
    const error = result.error || new Error(`${label} exited with code ${result.status}. See ${taskLog}.`);
    error.taskLog = taskLog;
    throw error;
  }
  progress.stop(`${label} completed`);
}
run.sequence = 1;

async function runGradle(tasks, version, label, env, dryRun) {
  await run(
    gradleCommand,
    [...tasks, `-PreleaseVersion=${version}`, "--console=plain"],
    label,
    env,
    dryRun,
  );
}

function validateSelection(targets, env, properties) {
  const errors = [];
  if ((targets.includes("modrinth") || targets.includes("description")) && !env.MODRINTH_TOKEN) {
    errors.push("MODRINTH_TOKEN is missing from .env.");
  }
  if (targets.includes("curseforge") && !env.CURSEFORGE_TOKEN) {
    errors.push("CURSEFORGE_TOKEN is missing from .env.");
  }
  if (
    targets.includes("curseforge") &&
    !env.CURSEFORGE_PROJECT_ID &&
    !properties.get("curseforge_project_id")
  ) {
    errors.push("CURSEFORGE_PROJECT_ID is missing from .env or gradle.properties.");
  }
  for (const file of [".public/changelog.md", ".public/release-header.md"]) {
    if (targets.includes("github") && !existsSync(join(root, file))) {
      errors.push(`${file} is missing.`);
    }
  }
  if (targets.includes("description") && !existsSync(join(root, ".public", "description.md"))) {
    errors.push(".public/description.md is missing.");
  }
  if (errors.length) throw new Error(errors.join("\n"));
}

async function main() {
  intro("Played Release Center");
  const properties = readProperties(join(root, "gradle.properties"));
  const currentVersion = properties.get("mod_version");
  const minecraftVersion = properties.get("minecraft_version");
  const env = releaseEnvironment();

  const bump = unwrap(
    await select({
      message: `Choose the next version (current ${currentVersion})`,
      options: [
        { value: "patch", label: `Patch  ${bumpVersion(currentVersion, "patch")}` },
        { value: "minor", label: `Minor  ${bumpVersion(currentVersion, "minor")}` },
        { value: "major", label: `Major  ${bumpVersion(currentVersion, "major")}` },
        { value: "custom", label: "Custom version" },
      ],
    }),
  );
  const version =
    bump === "custom"
      ? unwrap(
          await text({
            message: "Enter the release version",
            placeholder: currentVersion,
            validate: (value) =>
              semverPattern.test(value.trim())
                ? undefined
                : "Use semantic versioning, for example 1.2.3 or 1.2.3-beta.1.",
          }),
        ).trim()
      : bumpVersion(currentVersion, bump);

  const targets = unwrap(
    await multiselect({
      message: "Select release actions",
      required: true,
      initialValues: ["modrinth", "curseforge", "github"],
      options: [
        { value: "modrinth", label: "Publish to Modrinth" },
        { value: "curseforge", label: "Publish to CurseForge" },
        { value: "github", label: "Create GitHub release" },
        { value: "description", label: "Sync Modrinth description" },
      ],
    }),
  );
  const changelogPath = join(root, ".public", "changelog.md");
  const publishTargets = targets.filter((target) => target !== "description");
  const releaseNotes =
    publishTargets.length && !existsSync(changelogPath)
      ? unwrap(
          await text({
            message: "Short release notes",
            placeholder: "Describe the most important changes",
            validate: (value) =>
              value.trim().length >= 3
                ? undefined
                : "Enter at least one short change.",
          }),
        ).trim()
      : null;
  const dryRun = unwrap(
    await confirm({
      message: "Dry run only?",
      initialValue: false,
    }),
  );

  note(
    [
      `Minecraft: ${minecraftVersion}`,
      `Version:   ${currentVersion} → ${version}`,
      `Actions:   ${targets.join(", ")}`,
      `Mode:      ${dryRun ? "dry run" : "publish"}`,
    ].join("\n"),
    "Release summary",
  );
  const approved = unwrap(
    await confirm({
      message: dryRun ? "Show the commands?" : "Start this release now?",
      initialValue: false,
    }),
  );
  if (!approved) stop();

	initializeReleaseLog({
		version,
		previousVersion: currentVersion,
		minecraftVersion,
		targets,
		dryRun,
		env,
	});
	note(activeLogDirectory, "Release logs");

  if (!dryRun && releaseNotes) {
    mkdirSync(dirname(changelogPath), { recursive: true });
    writeFileSync(changelogPath, `## Changes\n\n- ${releaseNotes}\n`, "utf8");
  }
  if (!dryRun) validateSelection(targets, env, properties);
  if (publishTargets.length) {
    await runGradle(["clean", "build"], version, "Build and test", env, dryRun);
  }
  if (targets.includes("description")) {
    await runGradle(["modrinthSyncBody"], version, "Sync Modrinth description", env, dryRun);
  }
  if (targets.includes("modrinth")) {
    await runGradle(["verifyModrinthRelease"], version, "Publish to Modrinth", env, dryRun);
  }
  if (targets.includes("curseforge")) {
    await runGradle(["curseforge"], version, "Publish to CurseForge", env, dryRun);
  }
  if (targets.includes("github")) {
    await runGradle(["githubRelease"], version, "Create GitHub release", env, dryRun);
  }
  if (publishTargets.length) {
    await runGradle(["persistReleasedVersion"], version, "Store released version", env, dryRun);
  }
	appendReleaseLog(`[${timestamp()}] Release completed successfully.\n`);
  outro(dryRun ? "Dry run finished. Nothing was changed." : `Played ${version} released successfully.`);
}

main().catch((error) => {
	appendReleaseLog(`[${timestamp()}] RELEASE FAILED\n${error?.stack || error?.message || String(error)}\n`);
  cancel(error?.message || String(error));
  process.exitCode = 1;
});
