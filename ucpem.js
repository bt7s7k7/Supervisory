/// <reference path="./.vscode/config.d.ts" />
// @ts-check

const { project, join, constants, run, github } = require("ucpem")
const { mkdir } = require("node:fs/promises")
const { dirname } = require("node:path")

project.prefix("src/main/java").res("bt7s7k7.supervisory",
	github("bt7s7k7/TreeBurst").res("bt7s7k7.treeburst")
)

project.use(
	github("bt7s7k7/TreeBurst").script("leaf-gen")
)

project.script("export-art", async () => {
	/** @type {{ input: string, output: string, configuration?: Record<string, boolean>, x: number, y: number, width: number, height: number }[]} */
	const images = [
		{
			input: "remote_terminal_unit.xcf",
			output: "block/remote_terminal_unit_side.png",
			x: 0, y: 0, width: 16, height: 16,
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
			x: 0, y: 0, width: 16, height: 16,
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
			x: 0, y: 0, width: 16, height: 16,
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
			x: 0, y: 0, width: 16, height: 16,
			configuration: {
				"indicators": true,
				"rtu": false,
				"plc": true,
				"plcColors": true,
			}
		},
	]

	/** @type {string[]} */
	const commands = []

	for (const image of images) {
		const input = join(constants.projectPath, "art", image.input)
		const output = join(constants.projectPath, "src/main/resources/assets/supervisory/textures", image.output)

		console.log({ input, output })

		await mkdir(dirname(output), { recursive: true })

		const script = `
			(let* (
  				(image (car (gimp-file-load RUN-NONINTERACTIVE "${input}" "${input}")))
			)
				${Object.entries(image.configuration ?? {}).map(([name, enabled]) => (
			`(gimp-drawable-set-visible (car (gimp-image-get-layer-by-name image "${name}")) ${enabled ? "1" : "0"})`
		)).join(" ")}
				(gimp-image-crop image ${image.width} ${image.height} ${image.x} ${image.y})
				(gimp-file-save RUN-NONINTERACTIVE image (car (gimp-image-flatten image)) "${output}" "${output}")
				(gimp-image-delete image)
			)
		`.replace(/\t|\n/g, "")

		commands.push("-b " + JSON.stringify(script))
	}

	await run(`gimp -i ${commands.join(" ")} -b '(gimp-quit 0)'`)
}, { desc: "Generates and correctly places PNGs from XCF files" })
