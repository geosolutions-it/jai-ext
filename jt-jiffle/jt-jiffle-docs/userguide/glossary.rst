Glossary
========

.. _image-scope:

image scope
  Refers to variables whose values persist throughout processing. You create an image-scope variable by declaring it
  within the :ref:`init block <init-block>`.

  See also :ref:`pixel scope <pixel-scope>`
  

.. _init-block:

init block
  A script element used to declare non-image variables that will have :ref:`image scope <image-scope>`. It takes the
  following form::

    init {
        foo = 0;
        bar = 2 * M_PI + sqrt(42);
        baz;
    }

  The init block is optional. When present, it must precede the body of the script and appear after the options block.
  There can only be one init block.

.. _pixel-scope:

pixel scope
  This is the default scope for non-image variables. Any value assigned to a pixel-scope variable is lost after each
  pixel is processed.
  
  See also :ref:`image scope <image-scope>`.


.. _runtime-object:

runtime object
  The executable version of a Jiffle script. This is a `POJO <http://en.wikipedia.org/wiki/Plain_Old_Java_Object>`_ that
  implements the JiffleRuntime interface.

