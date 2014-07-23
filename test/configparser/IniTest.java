package configparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import configparser.exceptions.IniParserException;
import configparser.exceptions.InvalidLine;
import configparser.exceptions.ParsingError;

public class IniTest
{
    public static final Path outputRoot = Paths.get("target", "test-classes");

    private static final Path resourcesRoot = Paths.get("test", "resources");

    private static final Path resPythonImplScript = resourcesRoot.resolve("configparser-rw.py");

    private static boolean compareOutputs(Path iniInput) throws IOException
    {
        Path pythonOutput = writePython(iniInput);
        Path javaOutput = writeJava(iniInput);

        return diff(pythonOutput, javaOutput);
    }

    private static boolean diff(Path former, Path latter) throws IOException
    {
        ProcessBuilder pb = new ProcessBuilder("diff", "-u", former.toString(), latter.toString());
        Process process = pb.start();
        int exitCode = -1;
        try
        {
            exitCode = process.waitFor();
        } catch (InterruptedException e)
        {
            process.destroy();
            throw new IOException(e);
        }

        if (exitCode > 0)
        {
            try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                String line;
                while ((line = stdout.readLine()) != null)
                    System.out.println(line);
            }

            return false;
        }

        return true;
    }

    private static Path writeJava(Path iniInput) throws IOException
    {
        Path javaOutput = outputRoot.resolve(iniInput.getFileName() + "-java");

        new Ini().read(iniInput).write(javaOutput);

        return javaOutput;
    }

    private static Path writePython(Path iniInput) throws IOException
    {
        Path pythonOutput = outputRoot.resolve(iniInput.getFileName() + "-python");

        ProcessBuilder pb = new ProcessBuilder(resPythonImplScript.toString(), iniInput.toString(),
                pythonOutput.toString());
        Process process = pb.start();
        try
        {
            process.waitFor();
        } catch (InterruptedException e)
        {
            process.destroy();
            throw new IOException(e);
        }

        return pythonOutput;
    }

    @Test
    public void docsExample() throws IOException
    {
        Path cfg = resourcesRoot.resolve("docs-example.cfg");
        try
        {
            writeJava(cfg);
            Assert.fail("Did not throw IniParserException");
        } catch (IniParserException e)
        {
            List<ParsingError> parsingErrors = e.getParsingErrors();
            if (!(parsingErrors.size() == 1 && parsingErrors.contains(new InvalidLine(20, "key_without_value"))))
            {
                Assert.fail("Non-expected ParsingErrors were generated: " + e);
            }
        }
    }

    @Test
    public void docsExampleDefault() throws IOException
    {
        Path cfg = resourcesRoot.resolve("docs-example-default.cfg");
        Assert.assertTrue("The outputs of python and java differ", compareOutputs(cfg));
    }
}