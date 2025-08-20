
package fiji.plugin.imaging_fcs;

import ij.ImageJ;
import fiji.plugin.imaging_fcs.imfcs.ImagingFCS;
import static org.junit.Assert.assertTrue;

public class Test {

    @org.junit.Test
    public void testApp() {

        // Simple test that always passes
        assertTrue(true);

        ImageJ ij = new ImageJ();

        ImagingFCS pluginImFCS = new ImagingFCS();

        pluginImFCS.run("start");

        // Prevent ImageJ from closing immediately by adding sleep
        try {
            Thread.sleep(1000000); // Keep the UI open for 10 seconds
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle the interruption
        }

        System.out.println("getInfo: " + ij.getInfo());

    }

}
