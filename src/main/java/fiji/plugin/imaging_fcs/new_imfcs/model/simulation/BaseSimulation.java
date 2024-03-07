package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagescience.random.GaussianGenerator;
import imagescience.random.PoissonGenerator;
import imagescience.random.UniformGenerator;

import java.util.Arrays;

public class BaseSimulation {
    private static final double PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR = Math.pow(10, 6);
    private static final double OBSERVATION_WAVELENGTH_CONVERSION_FACTOR = Math.pow(10, 9);
    private final SimulationModel model;
    private final ExpSettingsModel settingsModel;

    public BaseSimulation(SimulationModel model, ExpSettingsModel settingsModel) {
        this.model = model;
        this.settingsModel = settingsModel;
    }

    public int checkInDomain(double px, double py, double[][] domains, int[][][] domainsorted, int maxct,
                             double gridMidPos, double subgridsize) {
        // px and py are particle positions
        // domains has the position and size of the different existing domains
        // domainsorted is essentially a grid in which the domains are sorted according
        // to their place in the simulated area; this makes sure not all domains but
        // only domians in the vicinity of the particle are searched
        // maxct is the maximum number of domains in any subgrid area (max length of
        // domainsorted[][])
        // gridMidPos and subgridsize define the subgrid on which the domains were
        // sorted
        int maxxt = domainsorted.length;
        int maxyt = domainsorted[0].length;
        int xt = (int) Math.floor((px + gridMidPos) / subgridsize);
        int yt = (int) Math.floor((py + gridMidPos) / subgridsize);
        int result = 0;
        if (xt > maxxt || xt < 0 || yt > maxyt || yt < 0) {
            return result;
        }
        boolean indomain = false;
        int ct = 0;
        while (domainsorted[xt][yt][ct] > 0 && ct < maxct && !indomain) { // check whether particle is in domain, and if
            // yes, remember the domain number
            if (Math.pow(px - domains[domainsorted[xt][yt][ct]][0], 2.0)
                    + Math.pow(py - domains[domainsorted[xt][yt][ct]][1], 2.0)
                    - Math.pow(domains[domainsorted[xt][yt][ct]][2], 2.0) <= 0.0) {
                result = domainsorted[xt][yt][ct]; // remember number of domain in which particle resides
                indomain = true;
                if (Math.pow(px - domains[domainsorted[xt][yt][ct]][0], 2.0)
                        + Math.pow(py - domains[domainsorted[xt][yt][ct]][1], 2.0)
                        - Math.pow(domains[domainsorted[xt][yt][ct]][2], 2.0) == 0.0) {
                }
            }
            ct++;
        }
        return result;
    }

    public void simulateACF2D() {
        double tStep = model.getFrameTime() / model.getStepsPerFrame();
        double darkF = model.getKoff() / (model.getKoff() + model.getKon()); // fraction of molecules in the dark state
        if (model.getDoutDinRatio() <= 0) {
            IJ.showMessage("Dout/Din <= 0 is not allowed");
            return;
        }

        double pixelSizeRealSize = settingsModel.getPixelSize() / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
        double wavelength = settingsModel.getEmLambda() / OBSERVATION_WAVELENGTH_CONVERSION_FACTOR;
        double sigma0 = settingsModel.getSigma();

        int numOfSeeds = 18;
        // array of simulation Seeds so that the random number generators are different
        int[] seedArray = new int[numOfSeeds];
        double pixelSize = pixelSizeRealSize / settingsModel.getMagnification(); // pixel size in object space
        double pSFSize = 0.5 * sigma0 * wavelength / settingsModel.getNA(); // PSF size
        double gridSize = model.getPixelNum() * pixelSize; // gridsize; i.e. the size of the pixel area of the detector
        double midPos = gridSize / 2.0; // middle position
        double sizeLL = -model.getExtFactor() * gridSize; // lower limit of the simulation area
        double sizeUL = model.getExtFactor() * gridSize; // upper limit of the simulation area
        double detectorSize = gridSize / 2.0; // the detector extends from -detectorSize to detectorSize,
        // i.e. it is the same as gridSize
        double bleachFactor = 2.0; // the 2.0 ensures that no bleaching happens if tauBleach is 0
        if (model.getTauBleach() != 0) {
            bleachFactor = Math.exp(-tStep / model.getTauBleach());
        }
        double[] blinkFactor = new double[2];
        blinkFactor[0] = Math.exp(-tStep * model.getKon());
        blinkFactor[1] = Math.exp(-tStep * model.getKoff());

        double gridLength = (sizeUL - sizeLL); // length of full simualtion grid
        double gridMidPos = gridLength / 2; // half length of full simulation grid
        int numberofdomains = (int) Math.ceil(Math.pow(gridLength * Math.pow(10, 6), 2) * model.getDomainDensity());
        double[][] domains = new double[numberofdomains][3];
        int subgridnum = (int) Math.ceil(gridLength / (model.getDomainRadius() * 10)) + 1; // number N of elements in a NxN
        // array
        double subgridsize = gridLength / (subgridnum - 1); // gridsize in the NxN array
        int maxdomainperarea = (int) Math.ceil(Math.pow(subgridsize / (model.getDomainRadius() * 0.5), 2.0));
        int maxct = 0;
        int[][][] domainsorted = new int[1][1][1]; // subgridsize defines a grid with sizes larger than the largest
        // domain radius
        int[][][] dsortmp = new int[subgridnum][subgridnum][maxdomainperarea];
        int[][] dctr = new int[subgridnum][subgridnum]; // temporary counter
        int num1 = (int) Math.round(model.getNumParticles() * (1 - model.getF2() - model.getF3())); // divide particle into their types according
        // to their fractions (1- F2 - F3), F2, F3
        int num2 = (int) Math.round(model.getNumParticles() * model.getF2());
        double[][] particles = new double[model.getNumParticles()][5]; // array for particle positions (0:x, 1:y), whether
        // particle is bleached (2) or in dark state (4) and if
        // particle is in domain then (3) contains domain number
        ImagePlus impSim = IJ.createImage("2D Simulation", "GRAY16", model.getPixelNum(), model.getPixelNum(), model.getNumFrames());

        if (model.getSeed() == 0) {
            Arrays.fill(seedArray, 0);
        } else {
            for (int x = 0; x < numOfSeeds; x++) {
                seedArray[x] = model.getSeed() + (int) Math.pow(x, 2.0);
            }
        }

        int cs = 0;
        UniformGenerator rugxpos = new UniformGenerator(sizeLL, sizeUL, seedArray[cs++]);
        UniformGenerator rugypos = new UniformGenerator(sizeLL, sizeUL, seedArray[cs++]);
        UniformGenerator ruig = new UniformGenerator(seedArray[cs++]);
        UniformGenerator rugbf = new UniformGenerator(seedArray[cs++]);
        UniformGenerator rugpin = new UniformGenerator(seedArray[cs++]);
        UniformGenerator rugpout = new UniformGenerator(seedArray[cs++]);
        UniformGenerator rugphop = new UniformGenerator(seedArray[cs++]);
        UniformGenerator rugblink = new UniformGenerator(seedArray[cs++]);
        GaussianGenerator rgg1 = new GaussianGenerator(0, Math.sqrt(2 * model.getD1() * tStep), seedArray[cs++]);
        GaussianGenerator rgg2 = new GaussianGenerator(0, Math.sqrt(2 * model.getD2() * tStep), seedArray[cs++]);
        GaussianGenerator rgg3 = new GaussianGenerator(0, Math.sqrt(2 * model.getD3() * tStep), seedArray[cs++]);
        GaussianGenerator rggpsf = new GaussianGenerator(0, pSFSize, seedArray[cs++]);
        PoissonGenerator rpgphoton = new PoissonGenerator(tStep * model.getCPS(), seedArray[cs++]);
        // PoissonGenerator rpgnoise = new PoissonGenerator(cameraNoiseFactor,
        // seedArray[cs++]);
        GaussianGenerator rggnoise = new GaussianGenerator(0, Math.sqrt(model.getCameraNoiseFactor()), seedArray[cs++]);
        GaussianGenerator rggdom1 = new GaussianGenerator(0, Math.sqrt(2 * model.getD1() / model.getDoutDinRatio() * tStep),
                seedArray[cs++]);
        GaussianGenerator rggdom2 = new GaussianGenerator(0, Math.sqrt(2 * model.getD2() / model.getDoutDinRatio() * tStep),
                seedArray[cs++]);
        GaussianGenerator rggdom3 = new GaussianGenerator(0, Math.sqrt(2 * model.getD3() / model.getDoutDinRatio() * tStep),
                seedArray[cs++]);
        GaussianGenerator rggdrad = new GaussianGenerator(model.getDomainRadius(), model.getDomainRadius() / 10, seedArray[cs++]);

        if (model.getIsDomain()) { // create domains
            int counter = 1; // the 0 position is not used
            int totcount = 0;
            IJ.showStatus("creating non-overlapping domains");

            while (counter < numberofdomains && totcount < 10 * numberofdomains) {
                if (Thread.currentThread().isInterrupted()) {
                    IJ.showStatus("Simulation Interrupted");
                    IJ.showProgress(1);
                    return;
                }
                domains[counter][0] = rugxpos.next();
                domains[counter][1] = rugypos.next();
                domains[counter][2] = rggdrad.next();
                for (int x = 0; x < counter; x++) { // check that domains do not overlap; if there is overlap create a
                    // new domain
                    if (Math.pow(domains[counter][0] - domains[x][0], 2) + Math.pow(domains[counter][1] - domains[x][1],
                            2) < Math.pow(domains[counter][2] + domains[x][2], 2)) {
                        x = counter--;
                    }
                }
                counter++;
                totcount++;
                IJ.showProgress(counter, numberofdomains);
            }

            if (totcount >= 10 * numberofdomains) {
                IJ.showMessage("Domains too dense, cannot place them without overlap.");
                IJ.showStatus("Simulation Error");
                IJ.showProgress(1);
                return;
            }

            maxct = 0;
            for (int x = 1; x < numberofdomains; x++) {
                int xt = (int) Math.floor((domains[x][0] + gridMidPos) / subgridsize);
                int yt = (int) Math.floor((domains[x][1] + gridMidPos) / subgridsize);
                dsortmp[xt][yt][dctr[xt][yt]++] = x; // dsortmp stores the number of each domain whose centre is in a
                // particular grid area
                if (dctr[xt][yt] > maxct) {
                    maxct = dctr[xt][yt]; // maximum number of domains detected in any grid
                }
            }

            maxct *= 9; // the domains of 9 neighbouring pixels are combined into one grid, so the
            // maximum number increases accordingly

            domainsorted = new int[subgridnum][subgridnum][maxct]; // domains will be sorted into a grid for faster
            // testing whether particles are in domains

            for (int x = 0; x < subgridnum; x++) { // domainsorted contains for each grid position all domains in that
                // and all directly surrounding grid positions
                for (int y = 0; y < subgridnum; y++) { // as the grid is larger than a domain radius, a particle in that
                    // grid can be only in any of these domains if at all
                    int dct = 0;
                    int dtmp = 0;
                    while (dsortmp[x][y][dtmp] > 0) {
                        domainsorted[x][y][dct++] = dsortmp[x][y][dtmp++];
                    }
                    dtmp = 0;
                    if ((x + 1) < subgridnum) {
                        while (dsortmp[x + 1][y][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x + 1][y][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((y + 1) < subgridnum) {
                        while (dsortmp[x][y + 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x][y + 1][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((x + 1) < subgridnum && (y + 1) < subgridnum) {
                        while (dsortmp[x + 1][y + 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x + 1][y + 1][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((x - 1) >= 0) {
                        while (dsortmp[x - 1][y][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x - 1][y][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((y - 1) >= 0) {
                        while (dsortmp[x][y - 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x][y - 1][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((x - 1) >= 0 && (y - 1) >= 0) {
                        while (dsortmp[x - 1][y - 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x - 1][y - 1][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((x + 1) < subgridnum && (y - 1) >= 0) {
                        while (dsortmp[x + 1][y - 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x + 1][y - 1][dtmp++];
                        }
                    }
                    dtmp = 0;
                    if ((x - 1) >= 0 && (y + 1) < subgridnum) {
                        while (dsortmp[x - 1][y + 1][dtmp] > 0) {
                            domainsorted[x][y][dct++] = dsortmp[x - 1][y + 1][dtmp++];
                        }
                    }
                }
            }
        }

        // determine intial positions for all particles
        for (int m = 0; m < model.getNumParticles(); m++) {
            particles[m][0] = rugxpos.next();
            particles[m][1] = rugypos.next();
            particles[m][2] = 1.0;
            if (model.getBlinkFlag()) {
                if ((int) ((m + 1) * darkF) > (int) (m * darkF)) {
                    particles[m][4] = 0.0;
                } else {
                    particles[m][4] = 1.0;
                }
            } else {
                particles[m][4] = 1.0;
            }
        }

        if (model.getIsDomain()) { // check for each particle whether it is in a domain
            for (int m = 0; m < model.getNumParticles(); m++) {
                particles[m][3] = checkInDomain(particles[m][0], particles[m][1], domains, domainsorted, maxct,
                        gridMidPos, subgridsize);
            }
        }

        IJ.showStatus("Simulating ...");
        for (int n = 0; n < model.getNumFrames(); n++) { // runCPU over all time steps/frames
            // check for interruption and stop execution
            if (Thread.currentThread().isInterrupted()) {
                IJ.showStatus("Simulation Interrupted");
                IJ.showProgress(1);
                return;
            }

            ImageProcessor ipSim = impSim.getStack().getProcessor(n + 1); // get the image processor

            for (int dx = 0; dx < model.getPixelNum(); dx++) { // add the camera offset and a noise term to each pixel
                for (int dy = 0; dy < model.getPixelNum(); dy++) {
                    ipSim.putPixelValue(dx, dy, model.getCameraOffset() + rggnoise.next());
                }
            }

            if (n == model.getBleachFrame()) { // if bleach frame is reached, bleach all particles within the bleach region
                for (int m = 0; m < model.getNumParticles(); m++) {
                    if (Math.sqrt(Math.pow(particles[m][0], 2.0) + Math.pow(particles[m][1], 2.0)) < model.getBleachRadius()) {
                        particles[m][2] = 0.0;
                    }
                }
            }

            for (int s = 0; s < model.getStepsPerFrame(); s++) { // there will be stepsPerFrame for each frame simulated
                for (int m = 0; m < model.getNumParticles(); m++) { // change positions of all particles

                    double dx = 0; // step sizes
                    double dy = 0;

                    if (!model.getIsDomain() && !model.getIsMesh()) {

                        if (m < num1) { // if there are no domains and no mesh then diffuse freely
                            dx = rgg1.next();
                            dy = rgg1.next();
                        } else if (m < num1 + num2) {
                            dx = rgg2.next();
                            dy = rgg2.next();
                        } else if (m >= num1 + num2) {
                            dx = rgg3.next();
                            dy = rgg3.next();
                        }

                    } else if (!model.getIsDomain() && model.getIsMesh()) { // simulate diffusion on a simple meshwork grid

                        if (m < num1) { // if there are no domains and no mesh then diffuse freely
                            dx = rgg1.next();
                            dy = rgg1.next();
                        } else if (m >= num1 && m < num1 + num2) {
                            dx = rgg2.next();
                            dy = rgg2.next();
                        } else if (m >= num1 + num2) {
                            dx = rgg3.next();
                            dy = rgg3.next();
                        }

                        boolean hoptrue = model.getHopProbability() > rugphop.next() || model.getHopProbability() == 1;

                        if (!hoptrue) { // if hop is not true, step inside the mesh only
                            while ((Math.floor(particles[m][0] / model.getMeshWorkSize()) != Math
                                    .floor((particles[m][0] + dx) / model.getMeshWorkSize()))) {
                                if (m < num1) {
                                    dx = rgg1.next();
                                } else if (m >= num1 && m < num1 + num2) {
                                    dx = rgg2.next();
                                } else if (m >= num1 + num2) {
                                    dx = rgg3.next();
                                }
                            }
                            while ((Math.floor(particles[m][1] / model.getMeshWorkSize()) != Math
                                    .floor((particles[m][1] + dy) / model.getMeshWorkSize()))) {
                                if (m < num1) {
                                    dy = rgg1.next();
                                } else if (m >= num1 && m < num1 + num2) {
                                    dy = rgg2.next();
                                } else if (m >= num1 + num2) {
                                    dy = rgg3.next();
                                }
                            }
                        }

                    } else if (model.getIsDomain() && !model.getIsMesh()) { // if there are domains, determine for each particle
                        // whether it is in a domain and have it diffuse
                        // accordingly

                        int domnum = (int) particles[m][3];

                        if (domnum != 0) { // if the particle is in a domain, the next step size is determined by domain
                            // diffusion
                            if (m < num1) { // if there is no border crossing this would be the 1. case of diffusion
                                // within a domain (in->in)
                                dx = rggdom1.next();
                                dy = rggdom1.next();
                            } else if (m >= num1 && m < num1 + num2) {
                                dx = rggdom2.next();
                                dy = rggdom2.next();
                            } else if (m >= num1 + num2) {
                                dx = rggdom3.next();
                                dy = rggdom3.next();
                            }
                        } else { // if the particle is not in a domain, the next step size is determined by
                            // diffusion of the surrounding matrix
                            if (m < num1) { // if there is no border crossing this would be the 2. case of diffusion
                                // outside domains (out->out)
                                dx = rgg1.next();
                                dy = rgg1.next();
                            } else if (m >= num1 && m < num1 + num2) {
                                dx = rgg2.next();
                                dy = rgg2.next();
                            } else if (m >= num1 + num2) {
                                dx = rgg3.next();
                                dy = rgg3.next();
                            }
                        }

                        boolean crossinout = false; // are crossings allowed?
                        boolean crossoutin = false;
                        if (model.getPout() > rugpout.next() || model.getPout() == 1.0) {
                            crossinout = true;
                        }
                        if (model.getPin() > rugpin.next() || model.getPin() == 1.0) {
                            crossoutin = true;
                        }

                        if (domnum != 0 && crossinout) { // if inside domain and in-out allowed
                            int domcheck = checkInDomain(particles[m][0] + dx, particles[m][1] + dy, domains,
                                    domainsorted, maxct, gridMidPos, subgridsize); // is new position in domain
                            if (domcheck == 0) { // act only if new position is actually outside domain
                                double domx = domains[domnum][0]; // domain coordinates
                                double domy = domains[domnum][1];
                                double domr = domains[domnum][2];
                                double px = particles[m][0] - domx;
                                double py = particles[m][1] - domy;
                                double sol = (-(px * dx + py * dy) + Math.sqrt(-Math.pow(py * dx - px * dy, 2.0)
                                        + Math.pow(dx * domr, 2.0) + Math.pow(dy * domr, 2.0)))
                                        / (Math.pow(dx, 2.0) + Math.pow(dy, 2.0));
                                // move the particle to the border with Din and outside with Dout
                                dx = (sol + (1 - sol) * Math.sqrt(model.getDoutDinRatio())) * dx;
                                dy = (sol + (1 - sol) * Math.sqrt(model.getDoutDinRatio())) * dy;
                            }
                        }

                        if (domnum != 0 && !crossinout) { // if inside domain and in-out not allowed
                            double domx = domains[domnum][0]; // domain coordinates
                            double domy = domains[domnum][1];
                            double domr = domains[domnum][2];
                            // find (dx, dy) to stay in domain
                            while (Math.pow(particles[m][0] + dx - domx, 2.0)
                                    + Math.pow(particles[m][1] + dy - domy, 2.0) > Math.pow(domr, 2.0)) {
                                if (m < num1) {
                                    dx = rggdom1.next();
                                    dy = rggdom1.next();
                                } else if (m >= num1 && m < num1 + num2) {
                                    dx = rggdom2.next();
                                    dy = rggdom2.next();
                                } else if (m >= num1 + num2) {
                                    dx = rggdom3.next();
                                    dy = rggdom3.next();
                                }
                            }
                        }

                        if (domnum == 0 && crossoutin) { // if outside domain and out-in allowed
                            int domcheck = checkInDomain(particles[m][0] + dx, particles[m][1] + dy, domains,
                                    domainsorted, maxct, gridMidPos, subgridsize); // is new position in domain
                            if (domcheck != 0) { // act only if step brings particle into domain
                                double domx = domains[domcheck][0]; // domain coordinates
                                double domy = domains[domcheck][1];
                                double domr = domains[domcheck][2];
                                double px = particles[m][0] - domx;
                                double py = particles[m][1] - domy;
                                double sol = (-(px * dx + py * dy) - Math.sqrt(-Math.pow(py * dx - px * dy, 2.0)
                                        + Math.pow(dx * domr, 2.0) + Math.pow(dy * domr, 2.0)))
                                        / (Math.pow(dx, 2.0) + Math.pow(dy, 2.0));
                                // move the particle to the border with Dout and inside with Din
                                dx = (sol + (1 - sol) / Math.sqrt(model.getDoutDinRatio())) * dx;
                                dy = (sol + (1 - sol) / Math.sqrt(model.getDoutDinRatio())) * dy;
                            }
                        }

                        if (domnum == 0 && !crossoutin) { // if outside domain and out-in not allowed
                            while (checkInDomain(particles[m][0] + dx, particles[m][1] + dy, domains, domainsorted,
                                    maxct, gridMidPos, subgridsize) != 0) { // find (dx, dy) to stay outside domain
                                if (m < num1) {
                                    dx = rgg1.next();
                                    dy = rgg1.next();
                                } else if (m >= num1 && m < num1 + num2) {
                                    dx = rgg2.next();
                                    dy = rgg2.next();
                                } else if (m >= num1 + num2) {
                                    dx = rgg3.next();
                                    dy = rgg3.next();
                                }
                            }
                        }

                    } else if (model.getIsDomain() && model.getIsMesh()) {
                        IJ.showMessage("Mesh and Domain diffusion has not been implemented yet");
                        IJ.showStatus("Done");
                        return;
                    }

                    particles[m][0] += dx; // finalize step
                    particles[m][1] += dy;

                    if (rugbf.next() > bleachFactor) {
                        particles[m][2] = 0.0;
                    }

                    int index = (int) particles[m][4]; // blinking of particles
                    if (model.getBlinkFlag()) {
                        if (rugblink.next() > blinkFactor[index]) {
                            particles[m][4] = Math.abs(1.0 - particles[m][4]);
                        }
                    }

                    if (particles[m][0] > sizeUL || particles[m][1] > sizeUL || particles[m][0] < sizeLL
                            || particles[m][1] < sizeLL) {
                        // Reset particle on border if particle left the simulation region
                        int tmp1 = (int) Math.floor(ruig.next() + 0.5);
                        int tmp2 = (int) (1 - 2 * Math.floor(ruig.next() + 0.5));
                        particles[m][0] = tmp1 * rugxpos.next() + (1 - tmp1) * tmp2 * sizeUL;
                        particles[m][1] = (1 - tmp1) * rugypos.next() + tmp1 * tmp2 * sizeUL;
                        particles[m][2] = 1.0;
                    }

                    if (model.getIsDomain()) { // check the domain location of the particle
                        particles[m][3] = checkInDomain(particles[m][0], particles[m][1], domains, domainsorted,
                                maxct, gridMidPos, subgridsize);
                    }
                    // create photons if the particle is fluorescent
                    int nop = (int) Math.round(rpgphoton.next() * particles[m][2] * particles[m][4]);
                    for (int p = 0; p < nop; p++) { // runCPU over emitted photons
                        double cordx = particles[m][0] + rggpsf.next();
                        double cordy = particles[m][1] + rggpsf.next();
                        if (cordx < detectorSize && cordy < detectorSize && cordx > -detectorSize
                                && cordy > -detectorSize) {
                            int tpx = (int) Math.floor((cordx + midPos) / pixelSize);
                            int tpy = (int) Math.floor((cordy + midPos) / pixelSize);
                            int tmp = (int) (ipSim.getPixelValue(tpx, tpy) + 1);
                            ipSim.putPixelValue(tpx, tpy, tmp);
                        }
                    } // end photon loop (p)
                } // end particle loop (m)
            } // end step per frame loop (s)
            IJ.showProgress(n, model.getNumFrames());
        } // end frame loop (n)

        /*
        if (!batchSim) {
            // show the simulation file
            System.arraycopy(newSimSettings, 0, settings, 0, nosimsettings); // save the settings used for the
            // simulation in settings
            ImagePlus img = (ImagePlus) impSim.clone();
            img.show();
            IJ.run(img, "Enhance Contrast", "saturated=0.35"); // autoscaling the contrast
            // file = true;
            // obtainImage();
            // closeWindows();
        } else {
            // save the simulation file
            String $fs = batchPath.getAbsolutePath().toString() + "/" + "sim" + model.getD1() * Math.pow(10, 12) + "-"
                    + model.getD2() * Math.pow(10, 12) + "-" + model.getF2() + ".tif";
            IJ.saveAsTiff(impSim, $fs);
        } */
    }

    public void simulateACF3D() {
        double tStep = model.getFrameTime() / model.getStepsPerFrame();
        double darkF = model.getKoff() / (model.getKoff() + model.getKon()); // fraction of molecules in the dark state

        double pixelSizeRealSize = settingsModel.getPixelSize() / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
        double wavelength = settingsModel.getEmLambda() / OBSERVATION_WAVELENGTH_CONVERSION_FACTOR;
        double sigma0 = settingsModel.getSigma();

        int numOfSeeds = 50;
        int[] seedArray = new int[numOfSeeds]; // array of simulation Seeds so that the random number generators are
        // different
        double pixelSize = pixelSizeRealSize / settingsModel.getMagnification(); // pixel size in object space
        double pSFSize = 0.5 * sigma0 * wavelength / settingsModel.getNA(); // PSF size
        double gridSize = model.getPixelNum() * pixelSize; // gridsize
        double midPos = gridSize / 2.0; // middle position
        double sizeLL = -model.getExtFactor() * gridSize; // lower limit of the simulation area
        double sizeUL = model.getExtFactor() * gridSize; // upper limit of the simulation area
        double detectorSize = gridSize / 2.0; // size of the observation areas
        double bleachFactor = 2.0; // the 2.0 ensures that no bleaching happens
        if (model.getTauBleach() != 0) {
            bleachFactor = Math.exp(-tStep / model.getTauBleach());
        }
        double[] blinkFactor = new double[2];
        blinkFactor[0] = Math.exp(-tStep * model.getKon());
        blinkFactor[1] = Math.exp(-tStep * model.getKoff());

        // division by 2 to yield the 1/sqrt(e) radius
        double lightSheetThickness = settingsModel.getSigmaZ() * wavelength / settingsModel.getNA() / 2.0;

        // lower and upper limit of the zdirection of the simulation volume
        double thicknessLL = -10.0 * lightSheetThickness;
        double thicknessUL = 10.0 * lightSheetThickness; // z dimension is 20 times the light sheet thickness

        // divide particle in to their types according to their fractions (1- F2 - F3),
        // F2, F3
        int num1 = (int) Math.ceil(model.getNumParticles() * (1 - model.getF2() - model.getF3()));
        int num2 = (int) Math.ceil(model.getNumParticles() * model.getF2());
        double[][] particles = new double[model.getNumParticles()][5]; // array for particle positions and whether particle is
        // bleached or in the dark state
        double zcor;
        // double zfac = Math.tan( Math.asin(nA/1.333) ); // factor describing the
        // spread of the PSF cross-section on the camera if the particle is not in the
        // focal plane
        double zfac = settingsModel.getNA() / Math.sqrt(1.776889 - Math.pow(settingsModel.getNA(), 2));
        ImagePlus impSim = IJ.createImage("3D Simulation", "GRAY16", model.getPixelNum(), model.getPixelNum(),
                model.getNumFrames());

        if (model.getSeed() == 0) {
            Arrays.fill(seedArray, 0);
        } else {
            for (int x = 0; x < numOfSeeds; x++) {
                seedArray[x] = model.getSeed() + (int) Math.pow(x, 2.0);
            }
        }

        int cs = 0;
        UniformGenerator rugxpos = new UniformGenerator(sizeLL, sizeUL, seedArray[cs++]);
        UniformGenerator rugypos = new UniformGenerator(sizeLL, sizeUL, seedArray[cs++]);
        UniformGenerator rugzpos = new UniformGenerator(thicknessLL, thicknessUL, seedArray[cs++]);
        UniformGenerator rugblink = new UniformGenerator(seedArray[cs++]);
        GaussianGenerator rgg1 = new GaussianGenerator(0, Math.sqrt(2 * model.getD1() * tStep), seedArray[cs++]);
        GaussianGenerator rgg2 = new GaussianGenerator(0, Math.sqrt(2 * model.getD2() * tStep), seedArray[cs++]);
        GaussianGenerator rgg3 = new GaussianGenerator(0, Math.sqrt(2 * model.getD3() * tStep), seedArray[cs++]);
        PoissonGenerator rpgphoton = new PoissonGenerator(tStep * model.getCPS(), seedArray[cs++]);
        GaussianGenerator rggnoise = new GaussianGenerator(0, Math.sqrt(model.getCameraNoiseFactor()), seedArray[cs++]);
        UniformGenerator dug1 = new UniformGenerator(0, 3, seedArray[cs++]);
        UniformGenerator dug2 = new UniformGenerator(0, 2, seedArray[cs++]);
        UniformGenerator rugbf = new UniformGenerator(0, 1, seedArray[cs++]);
        UniformGenerator BMU1 = new UniformGenerator(0, 1, seedArray[cs++]);
        UniformGenerator BMU2 = new UniformGenerator(0, 1, seedArray[cs++]);
        UniformGenerator BMU3 = new UniformGenerator(0, 1, seedArray[cs++]);
        UniformGenerator BMU4 = new UniformGenerator(0, 1, seedArray[cs++]);
        // determine intial positions for all particles
        for (int m = 0; m < model.getNumParticles(); m++) {
            particles[m][0] = rugxpos.next();
            particles[m][1] = rugypos.next();
            particles[m][2] = rugzpos.next();
            // particles[m][2] = 2*lightSheetThickness;
            particles[m][3] = 1;
            if (model.getBlinkFlag()) {
                if ((int) ((m + 1) * darkF) > (int) (m * darkF)) {
                    particles[m][4] = 0.0;
                } else {
                    particles[m][4] = 1.0;
                }
            } else {
                particles[m][4] = 1.0;
            }
        }

        for (int n = 0; n < model.getNumFrames(); n++) { // runCPU over all time steps/frames

            // check for interruption and stop execution
            if (Thread.currentThread().isInterrupted()) {
                IJ.showStatus("Simulation Interrupted");
                IJ.showProgress(1);
                return;
            }

            ImageProcessor ipSim = impSim.getStack().getProcessor(n + 1);

            for (int dx = 0; dx < model.getPixelNum(); dx++) { // add the camera offset and a noise term to each pixel
                for (int dy = 0; dy < model.getPixelNum(); dy++) {
                    ipSim.putPixelValue(dx, dy, model.getCameraOffset() + rggnoise.next());
                }
            }
            for (int s = 0; s < model.getStepsPerFrame(); s++) {
                for (int m = 0; m < model.getNumParticles(); m++) { // change positions of all particles for each time step
                    int numOfSeeds1 = m + 1;
                    if (m < num1) {
                        particles[m][0] += rgg1.next();
                        particles[m][1] += rgg1.next();
                        particles[m][2] += rgg1.next();
                        // particles[m][2] = 2*lightSheetThickness;
                    } else if (m < num1 + num2) {
                        particles[m][0] += rgg2.next();
                        particles[m][1] += rgg2.next();
                        particles[m][2] += rgg2.next();
                    } else if (m >= num1 + num2) {
                        particles[m][0] += rgg3.next();
                        particles[m][1] += rgg3.next();
                        particles[m][2] += rgg3.next();
                    }

                    if (particles[m][3] != 0.0) { // bleaching of particles
                        if (rugbf.next() > bleachFactor) {
                            particles[m][3] = 0.0;
                        }
                    }

                    int index = (int) particles[m][4]; // blinking of particles
                    if (model.getBlinkFlag()) {
                        if (rugblink.next() > blinkFactor[index]) {
                            particles[m][4] = Math.abs(1.0 - particles[m][4]);
                        }
                    }

                    if (particles[m][0] > sizeUL || particles[m][1] > sizeUL || particles[m][2] > thicknessUL
                            || particles[m][0] < sizeLL || particles[m][1] < sizeLL
                            || particles[m][2] < thicknessLL) {
                        // Reset particle on border
                        int tmp1 = (int) Math.ceil(dug1.next());
                        int tmp2 = (int) Math.ceil(dug2.next());
                        if (tmp1 == 1) {
                            particles[m][0] = rugxpos.next();
                            particles[m][1] = rugypos.next();
                            if (tmp2 == 1) {
                                particles[m][2] = thicknessLL;
                            } else {
                                particles[m][2] = thicknessUL;
                            }
                        } else if (tmp1 == 2) {
                            particles[m][0] = rugxpos.next();
                            particles[m][2] = rugzpos.next();
                            if (tmp2 == 1) {
                                particles[m][1] = sizeLL;
                            } else {
                                particles[m][1] = sizeUL;
                            }
                        } else {
                            particles[m][1] = rugypos.next();
                            particles[m][2] = rugzpos.next();
                            if (tmp2 == 1) {
                                particles[m][0] = sizeLL;
                            } else {
                                particles[m][0] = sizeUL;
                            }
                        }
                        particles[m][3] = 1.0; // * new particle is fluorescent
                    }
                    // factor describing the increase of the PSF at the focal plane for a particle not situated in the
                    // focal plane
                    zcor = (pSFSize + (Math.abs(particles[m][2]) * (zfac / 2)));
                    int nop = (int) Math.round((Math.abs(
                            rpgphoton.next() * Math.exp(-0.5 * Math.pow(particles[m][2] / lightSheetThickness, 2.0))
                                    * particles[m][3] * particles[m][4])));
                    if (nop < 0) { // no negative photons
                        nop = 0;
                    }
                    for (int p = 0; p < nop; p++) { // runCPU over emitted photons
                        double cordx = particles[m][0]
                                + zcor * (Math.sqrt(-2 * Math.log(BMU1.next()))) * Math.cos(2 * Math.PI * BMU2.next());
                        double cordy = particles[m][1]
                                + zcor * (Math.sqrt(-2 * Math.log(BMU3.next()))) * Math.cos(2 * Math.PI * BMU4.next());
                        if (cordx < detectorSize && cordy < detectorSize && cordx > -detectorSize
                                && cordy > -detectorSize) {
                            int tpx = (int) Math.floor((cordx + midPos) / pixelSize);
                            int tpy = (int) Math.floor((cordy + midPos) / pixelSize);
                            int tmp = (int) (ipSim.getPixelValue(tpx, tpy) + 1);
                            ipSim.putPixelValue(tpx, tpy, tmp);
                        }
                    } // end photon loop (p)
                } // end particle loop (m)
            } // end steps per frame loop (s)
            IJ.showProgress(n, model.getNumFrames());
        } // end frame loop (n)
        // save the settings used for the simulation in settings
        // System.arraycopy(newSimSettings, 0, settings, 0, nosimsettings);

        ImagePlus img = (ImagePlus) impSim.clone();
        img.show();
        IJ.run(img, "Enhance Contrast", "saturated=0.35"); // autoscaling the contrast
        // file = true;
        // obtainImage();
    }
}