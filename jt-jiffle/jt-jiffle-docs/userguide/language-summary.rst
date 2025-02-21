Language Summary
================

Structure of a Jiffle script
----------------------------

A script consists of the script body, optionally preceded by one or more *special blocks* which are used to declare
variables and control runtime options. We'll return to these blocks later (skip ahead to :ref:`special-blocks` if you
can't wait), but first let's look at the general features of the language.

Comments
--------

::

  /* 
   * C-style block comments
   * are supported
   * in jiffle
   */

  // As are line comments


Variables
---------

Types and variable declaration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Jiffle supports the following types of variables:

  **Scalar**
    A single value. In Jiffle all scalar values correspond to Java type Double.

  **Array**
    A dynamically sized array of scalar values.

  **Image**
    A variables that stands for a source or destination image in a script.

.. note::
   Support for multi-dimensional arrays is yet to be added.

Jiffle uses lazy declaration for scalar variables. In other words, you can just start using a variable name in the
script body. In this snippet::

  // The variable val required no prior declaration
  val = max(0, image1 - image2);

In contrast, array variables must be declared before use so that Jiffle can distinguish them from scalars::

  // declares an empty array 
  foo = [];

  // declares an array with initial values
  bar = [1, 2, 42];

Unlike languages such as Ruby, it is invalid to change the type type of a variable within a script::

  // Create an array variable
  foo = [1, 2, 3];

  // Now if you try to use it as a scalar you will get a compile-time error
  foo = 42;
  
  // Creating a scalar variable bar, then attempting to do an array operation
  // with it (<< is the append operator) will also make the compiler unhappy
  bar = 42;
  bar << 43; // error


Names
~~~~~

Variable names must begin with a letter, optionally followed by any combination of letters, digits, underscores and
dots. Letters can be upper or lower case. Variable names are case-sensitive.

See also :ref:`reserved-words`

.. _scope:

Scope
~~~~~

All scalar and list variables which first appear in the body of the script have *pixel-scope*: their values are
discarded after each destination pixel is processed. Variables declared in the init block, when present, have
*image-scope*: their values persist between pixels::

  init {
      // An image-scope variable with an initial value
      foo = 0;
  }

  // A variable which first appears in the script body
  // has pixel scope
  bar = 0;


Operators
---------

Arithmetic operators
~~~~~~~~~~~~~~~~~~~~

======  ==========================
Symbol  Description
======  ==========================
^       Raise to power 
\*      Multiply 
/       Divide 
%       Modulo (remainder) 
\+      Add 
\-      Subtract 
=       Assignment 
+=      Additive assignment 
-=      Subtractive assignment 
\*=     Multiplicative assignment 
/=      Divisive assignment 
%=      Modulo assignment 
======  ==========================

Logical operators
~~~~~~~~~~~~~~~~~

======  ==========================
Symbol  Description
======  ==========================
&&      logical AND 
||      logical OR 
^|      logical XOR 
==      equality test 
!=      inequality test 
>       greater than 
>=      greater than or equal to 
<=      less than 
<       less than or equal to 
!       logical complement 
======  ==========================

Ternary expression
~~~~~~~~~~~~~~~~~~

Example::

  // set foo to 1 if bar > 10; otherwise 0
  foo = bar > 10 ? 1 : 0;

See also :ref:`logical-functions`

Control flow
------------

If-else statements
~~~~~~~~~~~~~~~~~~

You can use the familiar if-else statement in a Jiffle script::

  if (foo > 0) n++ ;

  if (bar == 42) {
    result = 1;
  } else {
    result = 0;
  }


Loops
~~~~~

One of the features of Jiffle that makes for concise scripts is that you don't need to write the code to loop through
source and destination images because the runtime system does that for you. So many of your scripts will not need any
loop statements. However, Jiffle does provide loop constructs which are useful when working with pixel neighbourhoods or
performing iterative calculations.

foreach loop
++++++++++++

Probably most of the times when you need to use a loop in a Jiffle script it will be a foreach loop. The general form
is:

    foreach (*var* in *elements*) *target*

where: 
  *var* is a scalar variable that will be set to each value of *elements* in turn;

  *elements* is an array or sequence (see below);
  
  *target* is a single statement or a block of code delimited by curly brackets.

This example iterates through a 3x3 pixel neighbourhood and counts the number of values that are greater than a
threshold value. It uses **sequence** notation, which has the form **lowValue:highValue**. Each loop variable is set
to -1, 0, 1 in turn. The loop variables are then used to access a *relative pixel position* in the source image
(see :ref:`relative-pixel-position`)::

  // Iterate through pixels in a 3x3 neighbourhood
  n = 0;
  foreach (dy in -1:1) {
      foreach (dx in -1:1) {
          n += srcimage[dx, dy] > someValue;
      }
  }

Here is the same example, but this time using the **array** form of the foreach loop::

  // Iterate through pixels in a 3x3 neighbourhood
  delta = [-1, 0, 1];
  n = 0;
  foreach (dy in delta) {
      foreach (dx in delta) {
          n += srcimage[dx, dy] > someValue;
      }
  }


while loop
++++++++++

A conditional loop which executes the target statement or block while its conditional expression is non-zero::

  ynbr = y() - 500;
  total = 0;
  while (ynbr <= y() + 500) {
      xnbr = x() - 500;
      while (xnbr <= x() + 500) {
          total += srcimage[$xnbr, $ynbr];
          xnbr += 100;
      }
      ynbr += 100;
  }

until loop
++++++++++

A conditional loop which executes the target statement or block until its conditional expression is non-zero::

  ynbr = y() - 500;
  total = 0;
  until (ynbr > y() + 500) {
      xnbr = x() - 500;
      until (xnbr > x() + 500) {
          total += srcimage[$xnbr, $ynbr];
          xnbr += 100;
      }
      ynbr += 100;
  }

break and breakif statements
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Jiffle provides the **break** statement to unconditionally exit a loop::

  n = 0;
  foreach (i in 1:10) {
      if (foo[i] != null) {
          n++ ;
      } else {
          break;
      }
  }
  
There is also a **breakif** statement::

  n = 0;
  foreach (i in 1:10) {
      breakif(foo[i] == null);
      n++ ;
  }

Maximum loop iterations
~~~~~~~~~~~~~~~~~~~~~~~

In order to prevent loops from causing excessive resource consumption, Jiffle limits the maximum
number of loop iterations that will be executed before throwing an exception. This limit is applied
per pixel so that it is independent of the source image size. The default value is 200 and the
``it.geosolutions.jaiext.jiffle.maxIterations`` system property can be set to change this limit.
Setting this property to a negative value will disable this limit.

Functions
---------

General numeric functions
~~~~~~~~~~~~~~~~~~~~~~~~~

=======================  ====================   =====================  =======================  ===========================
Name                     Description            Arguments              Returns                  Notes
=======================  ====================   =====================  =======================  ===========================
``abs(x)``               Absolute value         double value           absolute value of x

``acos(x)``              Arc-cosine             value in range [-1,1]  angle in radians

``asin(x)``              Arc-sine               value in range [-1,1]  angle in radians

``atan(x)``              Arc-tangent            value in range [-1,1]  angle in radians

``atan2(x, y)``          Arc-tangent            x: double value        converts a rectangular
                                                y: double value        coordinate to polar and
                                                                       returns theta

``ceil(x)``              Ceiling                double value           smallest double >=x and
                                                                       is equal to an integer

``cos(x)``               Cosine                 angle in radians       cosine [-1, 1]

``degToRad(x)``          Degrees to radians     angle in radians       angle in degrees

``exp(x)``               Exponential            double value           e to the power x

``floor(x)``             Floor                  double value           integer part of x
                                                                       as a double

``IEEERemainder(x, y)``  Remainder              x: double value        remainder of x/y as
                                                y: double value        prescribed by IEEE 754

``isinf(x)``             Is infinite            double value           1 if x is positive
                                                                       or negative infinity;
                                                                       0 otherwise

``isnan(x)``             Is NaN                 double value           1 if x is equal to
                                                                       Java's Double.NaN;
                                                                       0 otherwise

``isnull(x)``            Is null                double value           1 if x is null;          Equivalent to isnan(x)
                                                                       0 otherwise

``log(x)``               Natural logarithm      positive value         logarithm to base e

``log(x, b)``            General logarithm      x: positive value;     logarithm to base b
                                                b: base
                                    
``radToDeg(x)``          Radians to degrees     angle in radians       angle in degrees

``rand(x)``              Pseudo-random number   double value           value in range [0, x)    Volatile function

``randInt(x)``           Pseudo-random number   double value           integer part of value    Equivalent to ``floor(rand(x))``
                                                                       in range [0, x)

``rint(x)``              Round                  double value           rounded value with half
                                                                       values rounded to the
                                                                       nearest even integer
                                                               
``round(x)``             Round                  double value           rounded value

``round(x, n)``          Round to multiple of   x: double value;       value rounded to         E.g. ``round(44.5, 10)``
                         n                      n: whole number        nearest multiple of n    returns 40
                 
``sin(x)``               Sine                   angle in radians       sine [-1, 1]

``sqrt(x)``              Square-root            non-negative value     square-root of x

``tan(x)``               Tangent                angle in radians       double value
=======================  ====================   =====================  =====================  ===========================

.. _logical-functions:

Logical functions
~~~~~~~~~~~~~~~~~

===================      ====================   =====================  =====================
Name                     Description            Arguments              Returns             
===================      ====================   =====================  =====================
``con(x)``               Conditional            double value           1 if x is non-zero;
                                                                       0 otherwise

``con(x, a)``            Conditional            double values          a if x is non-zero;
                                                                       0 otherwise

``con(x, a, b)``         Conditional            double values          a if x is non-zero;
                                                                       b otherwise

``con(x, a, b, c)``      Conditional            double values          a if x is positive;
                                                                       b if x is zero;
                                                                       c if x is negative

===================      ====================   =====================  =====================

Statistical functions
~~~~~~~~~~~~~~~~~~~~~

================  ====================   =====================  =========================
Name              Description            Arguments              Returns               
================  ====================   =====================  =========================
``max(x, y)``     Maximum                double values          maximum of x and y

``max(ar)``       Maximum                array                  maximum of array values 

``mean(ar)``      Mean                   array                  mean of array values

``min(x, y)``     Minimum                double values          minimum of x and y

``min(ar)``       Minimum                array                  minimum of array values

``median(ar)``    Median                 array                  median of array values

``mode(ar)``      Mode                   array                  mode of array values

``range(ar)``     Range                  array                  range of array values

``sdev(ar)``      Standard deviation     array                  sample standard deviation
                                                                of array values

``sum(ar)``       Sum                    array                  sum of array values

``variance(ar)``  Variance               array                  sample variance of array
                                                                values

================  ====================   =====================  =========================

Processing area functions
~~~~~~~~~~~~~~~~~~~~~~~~~

===============   ================================================
Name              Returns             
===============   ================================================
``height()``      Height of the processing area (world units)

``width()``       Width of the processing area (world units)

``xmin()``        Minimum X ordinate of the processing area (world units)

``ymin()``        Minimum Y ordinate of the processing area (world units)

``xmax()``        Maximum X ordinate of the processing area (world units)

``ymax()``        Maximum Y ordinate of the processing area (world units)

``x()``           X ordinate of the current processing position (world units)

``y()``           Y ordinate of the current processing position (world units)

``xres()``        Pixel width (world units)

``yres()``        Pixel height (world units)

===============   ================================================


.. _special-blocks:

Special blocks
--------------

The options block
~~~~~~~~~~~~~~~~~

Used to set options for Jiffle's runtime behaviour. Presently, only the *outside* option is supported. 

For example, this tells Jiffle to return a value of 0 for any pixel value request that falls outside the bounds of the
source image::

  options {
      outside = 0;
  }



The following script retrieves the maximum value in a 3x3 kernel centred on each source image and writes it to the
destination image. It uses the outside option to treat kernel locations beyond the source image's edge as null values
which will be ignored by the *max* function:

.. literalinclude:: /../src/main/resources/it/geosolutions/jaiext/jiffle/docs/MaxFilter.jfl

If the *outside* option is not set, any request for a value beyond an image's bounds will cause a JiffleRuntimeException.

The images block
~~~~~~~~~~~~~~~~

Used to associate variables with source (read-only) and destination (write-only) images. Example::

  images { 
      foo = read; 
      bar = read;
      result = write; 
  }
  
As shown in the above snippet, the block contains declarations of the form *name = (read | write)*. If this block is
provided, the Jiffle compiler expects that it contains declarations for all image variables used in the script. It not
provided, variable names can be defined as representing source or destination images using methods provided by the
Jiffle and JiffleBuilder classes. These methods are described further in :doc:`runtime`.

The init block
~~~~~~~~~~~~~~

This block declares variables that will have *image scope* during processing (as discussed in :ref:`scope`).

Each variable can optionally be assigned an intial value as ``foo`` is here::

  init {
      foo = 42;
      bar;
  }

If an initial value is not provided, one must be *injected* at run-time. See XXXX for more details.


Specifying source image position
--------------------------------

Pixel position and image band are specified using square bracket notation.

.. _relative-pixel-position:

Absolute pixel position
~~~~~~~~~~~~~~~~~~~~~~~

Absolute positions are specified using a ``$`` prefix (similar to the syntax used in some spreadsheet programs)::

  // Example: access the value at x=50 y=42
  value = srcimage[ $50, $42 ];

Variables and expressions can also appear in the brackets::

  value = srcimage[ $xpos, $(min(width() - 1, y() + 10)) ];

Relative pixel position
~~~~~~~~~~~~~~~~~~~~~~~

When values are not prefixed they are treated as offsets, relative to the current processing position::

  // Example: access the value at x+2, y-1
  value = srcimage[ 2, -1 ];

As with absolute positions, variables and expressions can also be used::

  value = srcimage[ dx, dy ];

Specifying the band 
~~~~~~~~~~~~~~~~~~~

The image band is specified as a single value, variable or expression in square brackets. It is always treated as an
absolute specifier::

  // Get value from band 2 at the current processing position
  value = srcimage[ 2 ];

As with pixel position, the band can be specified using a variable or an expression.

Specifying both pixel and band
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When specifying both band and pixel position, the band comes first::

  // Get the value for band 1, pixel position x=50, y=42
  value = srcimage[ 1 ][ $50, $42 ]

  // Get the value for band 1 at offset dx=-1, dy=3
  value = srcimage[ 1 ][ -1, 3 ]


.. _reserved-words:

Reserved words
--------------

The following are reserved words in Jiffle and may not be used as variable names:

  * boolean\ :sup:`a`
  * break
  * breakif
  * con
  * double\ :sup:`a`
  * else
  * false
  * float\ :sup:`a`
  * foreach
  * if
  * images
  * in
  * init
  * int\ :sup:`a`
  * null
  * options
  * read
  * true
  * until
  * while
  * write

:sup:`a` reserved for future use


