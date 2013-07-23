package org.geotools.renderedimage.viewer;

class TextTreeBuilder
{

    StringBuilder sb = new StringBuilder();

    int indent;

    public void newChild()
    {
        indent++;
        newLine();
    }

    public void endChild()
    {
        indent--;
    }

    public void newLine()
    {
        sb.append('\n');
        for (int i = 0; i < indent; i++)
        {
            sb.append("   ");
        }
    }

    public void append(String text)
    {
        sb.append(text);
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
