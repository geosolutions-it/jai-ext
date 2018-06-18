The Jiffle run-time system
==========================

Once you know how to *write* a Jiffle script, the next thing you'll want to do is *run* it. Jiffle provides a number of
ways to do that. All of them involve these basic steps:

#. Compile your script into a run-time object.
#. Provide the run-time object with source and destination images (and possibly coordinate information)
#. Execute the object.
#. Retrieve the results (images and/or summary values).

Although you write your script in the Jiffle language, you run it from within Java (or possibly another JVM language such
as Groovy).


Compiling and running scripts with JiffleBuilder
------------------------------------------------

Using JiffleBuilder is the easiest way to get started with Jiffle. Let's look at running the following script which :

.. literalinclude:: /../src/main/resources/it/geosolutions/jaiext/jiffle/docs/FilledCircle.jfl

This script implements a MAX filter: a 3x3 kernel is placed over each pixel in the input image, represented by the
variable *src*, and the maximum value found is written to the output image, represented by *dest* [*]_. 

Now let's look at a Java method which takes the script (in the form of a file) and an input image, and uses
JiffleBuilder to run the script, returning the resulting image to the caller.

.. literalinclude:: /../src/main/java/it/geosolutions/jaiext/jiffle/docs/FirstJiffleBuilderExample.java
   :language: java
   :start-after: // docs start method
   :end-before: // docs end method


Working with Jiffle objects directly
------------------------------------


Running scripts with JiffleExecutor
-----------------------------------


JiffleOpImage
-------------



.. [*] The variable names are arbitrary. We could have called them *foo* and *bar* if we had wanted to.


