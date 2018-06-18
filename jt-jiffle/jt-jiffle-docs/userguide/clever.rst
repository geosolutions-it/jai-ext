Cunning plans and clever tricks (plus some things to avoid)
===========================================================

Jiffle scripts with no destination images
-----------------------------------------

Jiffle allows you to run a script that has no destination images although there must be at least one source image. This
allows you to write scripts which calculate one or more properties from image values. The script below counts how many
values are greater than a threshold value:

.. literalinclude:: /../src/main/resources/it/geosolutions/jaiext/jiffle/docs/CountValuesGreaterThan.jfl

After running the script, the calling Java code can retrieve the result like this::

  Double count = runtimeObj.getVar("count");


Modifying behaviour at run-time
-------------------------------

Jiffle allows you to *inject* values for image scope variables at run-time. You can use this to do some nifty things. To
see how this works, let's start with a script that implements a mean filter for a 3x3 kernel:

.. literalinclude:: /../src/main/resources/it/geosolutions/jaiext/jiffle/docs/MeanFilter3x3.jfl 

Next, here is the script modified to allow the kernel size to be specified at run-time:

.. literalinclude:: /../src/main/resources/it/geosolutions/jaiext/jiffle/docs/MeanFilterDynamic.jfl 

Now at run-time, we can do this in the calling Java code::

  // Specify a 5x5 kernel by setting maxd to 2
  runtimeObj.setVar("maxd", 2);  
  

Saving the Java run-time source
-------------------------------

The Jiffle compiler translates an input script into a Java source code. After compiling the script you can get a copy of
the generated Java code to examine, modify or compile separately. Here's how to do it with JiffleBuilder:

.. literalinclude:: /../src/main/java/it/geosolutions/jaiext/jiffle/docs/GetRuntimeSource.java
   :language: java
   :start-after: // docs start getSourceFromJiffleBuilder
   :end-before: // docs end getSourceFromJiffleBuilder

You can also do the same thing when working with a Jiffle object directly:

.. literalinclude:: /../src/main/java/it/geosolutions/jaiext/jiffle/docs/GetRuntimeSource.java
   :language: java
   :start-after: // docs start getSourceFromJiffleObject
   :end-before: // docs end getSourceFromJiffleObject


Things to avoid
---------------

Don't depend on pixel processing order
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Say you want to create an image with sequential pixel values. Here's one approach::

  // image scope variable
  init { n = 0; }

  // write value to destination image and then increment
  dest = n++ ;

There are (at least) two traps waiting to snare you in this innocent looking code. The first is that the resulting image
depends on the order in which pixels are processed, which might not be the order you want. For example, at the time of
writing, a **JiffleDirectRuntime** object's **evaluateAll** method would have processed the pixels by column then row
(although this is deliberately not formalized in the run-time specifications).  Alternatively, your script could have
been executed by some application which instead called the **evaluate(x, y)** method directly and in some other order. 

The second trap has to do with the number of image tiles in the destination image, especially if the script is being
passed to a JAITools **JiffleOpImage**  for execution as part of an image rendering chain. In that case, the value of n
written to a pixel depends on which tile that pixel belongs to, and the order in which the tiles are processed.

So, unless you have complete control over the execution of the script, it's safer to specify how the values will be
ordered in the script itself. 

This script will give values ordered by column, then row::

  dest = x() + width() * y();

While this script gives values ordered by row, then column::

  dest = y() + x() * height();


