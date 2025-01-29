mist
====

Mist is a collection of experimental MIPS tools: disassembler, symbolic execution engine, recompiler and decompiler.
The primary focus is Allegrex CPU used in the PSP.

## Symbolic execution engine

This module can execute MIPS code symbolically and test whether functions from two different binaries behave in the same way.
The main goal was to check if it's possible to detect reverse engineering errors in the [uofw](https://github.com/uofw/uofw) project.
Overall the results were quite successful and this module allowed to find and fix RE errors in [few uofw modules](https://github.com/uofw/uofw/pulls?q=is%3Apr+author%3Akotcrab+is%3Aclosed).

This module uses [ghidra-rest-api](https://github.com/kotcrab/ghidra-rest-api) plugin to get symbols and types from Ghidra.

## Recompiler

Converts MIPS instructions into compilable C++ code. Currently under development, though the current version
already has most of the needed features.

## Disassembler

Disassembler converts program bytes into MIPS instructions so that they can be processed by other modules.

## Decompiler

This module is no longer maintained. While it was fun to do and a good learning experience
the existence of Ghidra makes most of this work obsolete.

If you're looking for a way to have proper support for PSP games in Ghidra then
see my [ghidra-allegrex](https://github.com/kotcrab/ghidra-allegrex) plugin.

![Showcase](https://i.imgur.com/m08iYuB.png)

Currently, the disassembly is automatically lifted into higher level representation and only basic
stack frame analysis is done. Further optimizations have to be invoked manually, those include:

- propagation of constants and variables
- renaming variables
- converting memory access to struct access
- converting address to string literals, global variables and function pointers
- simplifying branch conditions and eliminating dead branches
- expression evaluation
- specifying functions arguments

## See also

- [ghidra-allegrex](https://github.com/kotcrab/ghidra-allegrex) - Ghidra processor module adding support for the Allegrex CPU (PSP)
- [ghidra-rest-api](https://github.com/kotcrab/ghidra-rest-api) - Add read-only REST API to your Ghidra project 
