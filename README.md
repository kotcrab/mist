Mist is an interactive MIPS disassembler and decompiler.

### Notice

This project is no longer maintained. While it was fun to do and a good learning experience
the existence of Ghidra makes most of this work obsolete.

If you're looking for a way to have proper support for PSP games in Ghidra then
see my [ghidra-allegrex](https://github.com/kotcrab/ghidra-allegrex) plugin.

### About

![Showcase](https://i.imgur.com/m08iYuB.png)

As of now the tool is in a prototype / proof of concept state and not intended for public use.

Currently the disassembly is automatically lifted into higher level representation and only basic
stack frame analysis is done. Further optimizations have to be invoked manually, those include:

- propagation of constants and variables
- renaming variables
- converting memory access to struct access
- converting address to string literals, global variables and function pointers
- simplifying branch conditions and eliminating dead branches
- expression evaluation
- specifying functions arguments 
