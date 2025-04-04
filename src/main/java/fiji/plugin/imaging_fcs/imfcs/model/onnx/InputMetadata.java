package fiji.plugin.imaging_fcs.imfcs.model.onnx;

public class InputMetadata {
    long modelInputX;       // Corresponds to Width
    long modelInputY;       // Corresponds to Height
    long modelInputFrames;  // Corresponds to Frames
    
    public InputMetadata(long modelInputX, long modelInputY, long modelInputFrames) {
        this.modelInputX = modelInputX;
        this.modelInputY = modelInputY;
        this.modelInputFrames = modelInputFrames;
    }

    // Optional: Add a toString for easy printing/debugging
    @Override
    public String toString() {
        return "InputMetadata{" +
               "modelInputX=" + modelInputX +
               ", modelInputY=" + modelInputY +
               ", modelInputFrames=" + modelInputFrames +
               '}';
    }

	public String getX() {
        return String.valueOf(this.modelInputX);
	}

	public String getY() {
        return String.valueOf(this.modelInputY);
	}

	public String getFrames() {
        return String.valueOf(this.modelInputFrames);
	}
}

