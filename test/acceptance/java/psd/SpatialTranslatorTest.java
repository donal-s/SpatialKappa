package psd;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.demonsoft.spatialkappa.tools.SpatialTranslator;
import org.junit.Test;

public class SpatialTranslatorTest {

    private SpatialTranslator translator;
    
    @Test
    public void testTest1_1_dimerisationWithinIndividualVoxel() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-1-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-1-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_2_dimerisationAcrossReactionSurfaceWithinSingleCompartment() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-2-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-2-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_3_dimerisationAcrossReactionSurfaceBetweenCompartments() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-3-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-3-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_4_initialComplexesWithinSameCompartment() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-4-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-4-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_5_initialComplexesBetweenCompartments() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-5-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-5-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_6_buildPolymerPlaque() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-6-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-6-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_7_constructPentamerReceptors() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-7-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-7-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_8_allostericActivation() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-8-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-8-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @Test
    public void testTest1_9_spatialEGFRModel() throws Exception {
        translator = new SpatialTranslator(new File(TEST_DATA_DIRECTORY, "test-1-9-input.ka"));
        String testOutput = FileUtils.readFileToString(new File(TEST_DATA_DIRECTORY, "test-1-9-output.ka"));
        assertEquals(testOutput, translator.translateToKappa());
    }

    private static final String TEST_DATA_DIRECTORY = "test/acceptance/data/";
    
}
