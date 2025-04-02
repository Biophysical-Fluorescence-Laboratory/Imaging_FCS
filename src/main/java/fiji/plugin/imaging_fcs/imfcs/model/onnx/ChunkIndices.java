package fiji.plugin.imaging_fcs.imfcs.model.onnx;

class ChunkIndices {
    int startX;
    int endX;
    int startY;
    int endY;
    int startFrame;
    int endFrame;

    public ChunkIndices(int startX, int endX, int startY, int endY, int startFrame, int endFrame) {
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }
}
