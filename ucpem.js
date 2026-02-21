/// <reference path="./.vscode/config.d.ts" />
// @ts-check

const { project, join, constants, run, github, log } = require("ucpem")
const { mkdir, readFile, writeFile } = require("node:fs/promises")
const { dirname } = require("node:path")

project.prefix("src/main/java").res("bt7s7k7.supervisory",
	github("bt7s7k7/TreeBurst").res("bt7s7k7.treeburst")
)

project.use(
	github("bt7s7k7/TreeBurst").script("leaf-gen")
)

project.script("update-readme", async () => {
	const content = await readFile("docs/index.md", { encoding: "utf-8" })
	const types = JSON.parse(await readFile("docs-build/symbols.json", { encoding: "utf-8" }))
	const baseURL = new URL("https://bt7s7k7.github.io/Supervisory/")

	const stdURL = new URL("https://bt7s7k7.github.io/TreeBurst/")
	const stdTypes = await fetch(new URL("symbols.json", stdURL)).then(v => v.json())

	await writeFile("README.md", content.replace(/\{@link ([\w.]+)\}/g, (link, name) => {
		let url

		if (name in types) {
			url = new URL(types[name], baseURL)
		} else if (name in stdTypes) {
			url = new URL(stdTypes[name], stdURL)
		} else {
			log(`Failed to resolve symbol "${name}"`)
			return link
		}

		return `[\`${name}\`](${url.href})`
	}))
}, { desc: "Updates the README based on documentation index" })

async function exportGIMP(/** @type {{ input: string, output: string, configuration?: Record<string, boolean> }[]} */ images) {
	/** @type {string[]} */
	const commands = []

	for (const image of images) {
		const input = join(constants.projectPath, "art", image.input)
		const output = join(constants.projectPath, "src/main/resources/assets/supervisory/textures", image.output)

		log({ input, output })

		await mkdir(dirname(output), { recursive: true })

		const script = `
            (let* (
                (image (car (gimp-file-load RUN-NONINTERACTIVE "${input}" "${input}")))
            )
                ${Object.entries(image.configuration ?? {}).map(([name, enabled]) => (
			`(gimp-item-set-visible (car (gimp-image-get-layer-by-name image "${name}")) ${enabled ? "1" : "0"})`
		)).join(" ")}
                (gimp-file-save RUN-NONINTERACTIVE image "${output}" (car (gimp-image-merge-visible-layers image 1)))
                (gimp-image-delete image)
            )
        `.replace(/ {4}|\n/g, "")

		commands.push("-b " + JSON.stringify(script))
	}

	await run(`gimp --batch-interpreter=plug-in-script-fu-eval -i ${commands.join(" ")} -b '(gimp-quit 0)'`)
}

project.script("export-art", async () => {
	await exportGIMP([
		{
			input: "remote_terminal_unit.xcf",
			output: "block/remote_terminal_unit_side.png",
			configuration: {
				"indicators": false,
				"rtu": true,
				"plc": false,
				"plcColors": false,
			}
		},
		{
			input: "remote_terminal_unit.xcf",
			output: "block/remote_terminal_unit_front.png",
			configuration: {
				"indicators": true,
				"rtu": true,
				"plc": false,
				"plcColors": false,
			}
		},
		{
			input: "remote_terminal_unit.xcf",
			output: "block/programmable_logic_controller_side.png",
			configuration: {
				"indicators": false,
				"rtu": false,
				"plc": true,
				"plcColors": true,
			}
		},
		{
			input: "remote_terminal_unit.xcf",
			output: "block/programmable_logic_controller_front.png",
			configuration: {
				"indicators": true,
				"rtu": false,
				"plc": true,
				"plcColors": true,
			}
		},
		{
			input: "device_config_buffer.xcf",
			output: "item/device_config_buffer.png",
		}
	])
}, { desc: "Generates and correctly places PNGs from XCF files" })
