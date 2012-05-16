package psd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.demonsoft.spatialkappa.tools.SpatialTranslator;
import org.junit.Before;
import org.junit.Test;

//TODO - extension to allow mixed rule and agent locations

public class SpatialTranslatorTest {

    private SpatialTranslator translator;
    
    @Before
    public void setUp() throws Exception {
        translator = new SpatialTranslator("%compartment: cytosol [4]\n");
    }
    
    @Test
    public void testSpatialTranslatorFile() throws Exception {
        try {
            new SpatialTranslator((File) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-1-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-1-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    private static final String TEST_DATA_DIRECTORY = "test/acceptance/data/";
    
}
