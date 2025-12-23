# Supervisory

Provides industrial control and monitoring facilities for Minecraft inspired by real-world SCADA systems.

The modeled devices are the following:

  - **Remote Terminal Unit** (RTU): function as remote connectivity points; contain simple redstone I/O and enable PLCs to interface with remote blocks via network sockets
  - **Programmable Logic Controller** (PLC): fully programmable blocks which allow for complex logic execution, interacting with redstone, detecting block states and scanning inventories.

All programming is done using the TreeBurst language, which is purpose-built for this mod. Check out the [TreeBurst documentation](https://bt7s7k7.github.io/TreeBurst/) for more information, although the examples included should be enough to get understand the basics.

Supervisory is meant to be used with other mods, that provide logistics, industrial machines and complex crafting chains. Creating this mod, the main target was the [Create](https://modrinth.com/mod/create) mod, but you may find it useful for other uses too. 

## Comparison to CC:Tweaked

This mod is similar in purpose to [CC:Tweaked](https://modrinth.com/mod/cc-tweaked), allowing you to control your minecraft creations using code. Supervisory is a direct answer to my frustrations trying to create a SCADA type system called [CC-SCADA](https://github.com/bt7s7k7/CC-SCADA). 

Why you may want to use Supervisory over CC:Tweaked:

  - **Persistency**: all state is automatically persisted across chunk unloading
  - **Editing**: Supervisory features an in-game editor with mouse and clipboard support, allowing you to simple upload programs by pasting them in; an update is simple as `Ctrl-C` and `Ctrl-V`
  - **Reactive programming**: Supervisory features API for simple reactive programming, allowing you to specify dependencies and effects without boilerplate code
  - **No filesystem**: Program code is saved inside as part of the block, not in a file on disk, which removes clutter and prevents storage of useless data of destroyed computers
  - **Not using Lua**: Supervisory uses a purpose-built programming language with a C-like syntax for simple, compact and functional programming

This mod is most powerful when used in tandem with CC:Tweaked as it supports all peripherals, even those added by other mods.

## Mod Compatibility

For reading inventories, Supervisory use standard NeoForge capabilities, meaning it should be compatible with all mods. Additionally, explicit compatibility with CC:Tweaked is included, PLCs can use all CC:Tweaked peripherals, including those added by other mods, via the Interop API.

## Example

```js
// Create a reactive scope
reactive(\(ctx) {
	// Declare a dependency
	$chest = ctx.use(Storage.connect(LEFT))
	
	ctx.awaitReady()

	// Get information
	$stone = chest.countItems("minecraft:cobblestone")
	// Affect the world
	Redstone.setRight(stone > 32 ? 15 : 0)
})
```
