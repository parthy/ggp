For using the Prolog reasoner instead of JavaProver or Jocular you need to do the following:
- install Eclipse-Prolog (http://www.eclipse-clp.org)
- set the ECLIPSE_DIR environment variable to the Eclipse-Prolog installation directory
- use GameFactory.PROLOG in MyPlayer.commandStart(Message msg) instead of GameFactory.JAVAPROVER

It shouldn't be necessary to change any other part of your program.

