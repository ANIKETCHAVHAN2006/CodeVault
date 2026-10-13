# CodeVault

Compile and run (from this folder):

    mkdir -p out
    javac -d out $(find src -name "*.java")      # Linux / macOS
    java -cp out Main

Launch the graphical interface (Java Swing):

    java -cp out ui.CodeVaultUI

The graphical and console interfaces use the same `vault/` data files. You can
continue to launch the original console application with `java -cp out Main`.

Windows (PowerShell):

    mkdir out
    javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
    java -cp out Main

The `vault/` folder is created next to where you run the program.
First run asks you to create a master password.
"Day" = one program start. A new snippet is due on the next start (day + 1).
