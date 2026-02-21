<div style="text-align: center">
	<img src="docs/supervisory.png" style="image-rendering: pixelated;" alt="Supervisory brand image" width="100"></img>
</div>

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

For reading inventories, Supervisory use standard NeoForge capabilities, meaning it should be compatible with all mods. Additionally, explicit compatibility with CC:Tweaked is included, PLCs can use all CC:Tweaked peripherals, including those added by other mods, via the {@link Interop} API.

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
	Redstone.setRight(stone > 32)
})
```

## API Reference

See the [complete API reference here](https://bt7s7k7.github.io/Supervisory/reference.html).

## Guide

[Check out the guide on the project website to get proper code highlighting.](https://bt7s7k7.github.io/Supervisory/)

### Basics

The most important device in Supervisory is a Programmable Logic Controller (PLC), which allows you to program your logic for controlling things.

You can open the PLC configuration screen by right-clicking. The following GUI elements are present:

  1. Domain input
  2. Code editor
  3. Compile button
  4. Module selector
  5. Event log
  6. Command input

You can use the code editor to edit your code, but this is not recommended beyond basic changes. The intended approach is to edit your code with a proper editor on your computer and simply copy the code into the game via your clipboard. Check out the [VSCode extension](https://github.com/bt7s7k7/TreeBurst/tree/master/extension) for syntax highting. 

Each device can have multiple code modules, which are executed in order. This is intended as a way to separate your reusable library code from site specific configuration. Each button of the module selector has a tooltip explaining its function.

To actually run your code you should press the compile button. This will upload the code from your screen into the game, compile it and execute it. This is also how you save your code, if you close the screen without saving, your changes will be lost. Don't worry about doing this accidentally, an alert will notify you if you try to exit with unsaved changes. 

The device is factory reset every time your upload new code, so all state will be lost, including your variables, the device's local state, the event log etc.

The event log displays messages from the {@link print} function, compilation and runtime errors and lifecycle events.

You can use the command input to execute code without changing the program. The result of your input will be also printed to the log. All commands are executed in the global scope of your program so you can use and change variables and functions your have defined. You can use the up/down arrow to return to a previously executed command and enter to execute the command.

The use of the domain input will be specified in the next section. 

When a chunk unloads, all devices inside will be shutdown and then subsequently rebooted when the chunk loads. Due to this, all runtime state will be lost. However, the device's local state, accessed via {@link s}, the cache of network resources, accessed via {@link r} and the event log will be preserved. Of course the code will be preserved too.

The device allows for I/O via each of its six sides. Please note that the sides are specified from the perspective of the device, where the front is the part with the colored dots. If you configure the device with its front facing you, its right side will be on your left.

### Networking

Supervisory is based on a a publish-subscribe model. The indented architecture is that a PLC would connect to a socket to read information such as inventory content and publish the extracted values, which could then be listened for by other PLCs without introducing coupling to the actual source block.

An example of this architecture is a PLC connected to a chest, which publishes the cobblestone count. Devices listening for the count, do not need to know about the chest and do not need to be modified when the chest is moved or for example the cobblestone is moved into a storage drawer.

For devices to communicate between each other, they need to be connected to the same domain. Domains are ephemeral and immaterial; they are created when a device first connects to them and destroyed when the last device disconnects. Domains may include any characters in their name. For debugging, you can use the command `/supervisory monitor domain [name] true`, which will print all messages sent from devices on the domain to your chat. 

A device would read the inventory content and publish the results as follows:

```js
reactive(\(ctx) {
	$chest = ctx.use(Storage.connect(LEFT))
	ctx.awaitReady()
	r"cobblestone" = chest.countItems("minecraft:cobblestone")
})
```

By using the {@link r} function, you publish a resource, in this case a number, onto the network. The resource can subsequently be subscribed to as follows:

```js
reactive(\(ctx) {
	$count = ctx.use(r"cobblestone")
	ctx.awaitReady()

	Redstone.setRight(count < 32)
})
```

When a device first publishes a resource or its value changes it will broadcast the value onto its connected domain. All devices that subscribe to it will be notified and execute their relevant reactive scopes. The publishing device will also save the resource value.

When a device subscribes to a resource, it will broadcast a request for the resource's value. If a device has a value stored for said resource it will automatically respond. The receiving device will store the value in its cache.

This system means that in the first iteration of the reactive scope in the receiving device, the value of the resource will not be known, because the subscription was just created. The resource will have a value of {@link void}. That is what the {@link ReactiveScope.awaitReady} method is used for; if any of the resources used by the scope are not yet available, it will abort the execution of the scope, until their value becomes known. 

Keep in mind that the entire callback re-runs every time a resource changes. Since the list of monitored resources is dynamic, any new resources you access during an execution will replace the old ones in the next update cycle.

Of course the {@link reactive} API is optional, you can imperatively read values of resources via the {@link ReactiveDependency.prototype.value} field. In this case you'll have to handle missing values yourself.

### Interacting with the world

PLCs can interact with any block they directly touch. There are two ways of interactivity: redstone and sockets.

For redstone, you can use the {@link Redstone} API, which allows you to read and write redstone signals.

```js
reactive(\(ctx) {
	// Redstone inputs always read the signal strength,
	// returning a number from 0 to 15
	$a = ctx.use(Redstone.LEFT)	
	$b = ctx.use(Redstone.RIGHT)

	// You can write signals in a digital and an analog form
	Redstone.setBack(a && b)
	Redstone.setBackAnalog(a + b)
})
```

For more complex interaction, sockets are used. The PLC must explicitly connect to a socket which provides a resource. This proces takes some time or there may not be anything to connect to at all. For example when using the {@link Storage} API, when an inventory is not attached, the connection will not be established and the value of the resource will be {@link void}. Likewise for the {@link Sensor} API, when there isn't a block attached, it will also return {@link void}. Establishing a connection also takes a tick, so the first time connection is used, it will be {@link void}. To handle this, use the {@link ReactiveScope.awaitReady} method, same as with value resources.

```js
reactive(\(ctx) {
	$chest = ctx.use(Storage.connect(LEFT))
	ctx.awaitReady()

	print(chest)
})
```

Connections are automatically cached, so you don't have to worry about connecting to a socket multiple times or closing the connection. These things are handled for you.

### Interacting remotely

To extend the range of a PLC beyond its immediate vicinity, you can use an Remote Terminal Unit (RTU). Using an RTU you can provide sockets to connected blocks, which can then be used by PLC in the same domain. Similarly, you can read redstone signals and publish their values onto the network or create redstone signals controlled by a network resource.

When opening an RTU's configuration screen by right clicking it, you can see the following GUI elements:

  1. Domain input
  2. Side selection
  3. I/O name and type

By entering a domain name, the device will connect to it automatically. You can create a socket or bind a redstone signal by selecting the desired side, entering the socket/resource name and selecting the type of I/O.

Please note that the side selection only switches which side you are configuring. You can have I/O active on multiple sides, if you switch to a different side and then back you'll see your configuration remains.

It is also important to understand that the sides specified are from the perspective of the device, where the front is the part with the red dots. If you configure the device with its front facing you, its right side will be on your left, which is also reflected by the positioning of the buttons. 

For redstone, if you select `Signal In`, the device will publish the incoming redstone signal on the network in a resource of the name you specify. Similarly, if you select `Signal Out`, the device will generate a redstone signal with strength equal to the value of the resource of the specified name.

For sockets, the device won't do anything. It will only allow PLCs to connect to socket through it. For example if you place a chest on and RTU and configure a socket, you can use the {@link Storage.connect} function, in a PLC on the same domain with the name you specified, and connect to the chest as if it was attached directly to the PLC.

The PLC will not generate an error if a socket is not found, it will simply wait until an RTU providing a socket of a specified name connects to the domain. This is to handle cases when you move your RTU to a new location or when the chunk the RTU is in unloads. 

Multiple PLCs can be connected to the one socket on one RTU.

