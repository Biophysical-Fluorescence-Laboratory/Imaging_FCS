package fiji.plugin.imaging_fcs.imfcs.model.onnx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import fiji.plugin.imaging_fcs.imfcs.utils.Pair;

public class ChunkGenerator {
    private final int imageDimX;
    private final int imageDimY;
    private final int imageDimFrames;
    private final int modelInputX;
    private final int modelInputY;
    private final int modelInputFrames;
    private final int strideX;
    private final int strideY;
    private final int strideFrames;

    public ChunkGenerator(int imageDimX, int imageDimY, int imageDimFrames,
                   int modelInputX, int modelInputY, int modelInputFrames,
                   int strideX, int strideY, int strideFrames) {

        this.imageDimX = imageDimX;
        this.imageDimY = imageDimY;
        this.imageDimFrames = imageDimFrames;
        this.modelInputX = modelInputX;
        this.modelInputY = modelInputY;
        this.modelInputFrames = modelInputFrames;
        this.strideX = strideX;
        this.strideY = strideY;
        this.strideFrames = strideFrames;
    }

    public List<ChunkIndices> generateChunkIndicesList() {
        List<ChunkIndices> indicesList = new ArrayList<>();

        for (int x = 0; x <= imageDimX - modelInputX; x += strideX) {
            for (int y = 0; y <= imageDimY - modelInputY; y += strideY) {
                for (int frame = 0; frame <= imageDimFrames - modelInputFrames; frame += strideFrames) {
                    int endX = x + modelInputX;
                    int endY = y + modelInputY;
                    int endFrame = frame + modelInputFrames;
                    indicesList.add(new ChunkIndices(x, endX, y, endY, frame, endFrame));
                }
            }
        }
        return indicesList;
    }


    public ResultIndices mapChunkIndicesToResultIndices(ChunkIndices chunkIndices) {
        int resultX = chunkIndices.startX / strideX;
        int resultY = chunkIndices.startY / strideY;
        int resultFrame = chunkIndices.startFrame / strideFrames;
        return new ResultIndices(resultX, resultY, resultFrame);
    }

    public float[][][] generateResultArray() {
        System.out.println(imageDimX);
        System.out.println(imageDimY);
        System.out.println(imageDimFrames);
        System.out.println(strideX);
        System.out.println(strideY);
        System.out.println(strideFrames);

        int resultDimX = (imageDimX - modelInputX) / strideX + 1;
        int resultDimY = (imageDimY - modelInputY) / strideY + 1;
        int resultDimFrames = (imageDimFrames - modelInputFrames) / strideFrames + 1;
        
        System.out.println(resultDimX);
        System.out.println(resultDimY);
        System.out.println(resultDimFrames);
        return new float[resultDimX][resultDimY][resultDimFrames];
    }
    
    public Iterator<Pair<float[][][], ResultIndices>> getChunkIterator(float[][][] imageArr) {
      return new ChunkIterator(imageArr, this.modelInputX, this.modelInputY, this.modelInputFrames);
    }

    private class ChunkIterator implements Iterator<Pair<float[][][], ResultIndices>> {
      private final Iterator<ChunkIndices> indicesIterator;
      private final float[][][] imageArr;
      private final int modelInputX;
      private final int modelInputY;
      private final int modelInputFrames;

      public ChunkIterator(float[][][] imageArr, int modelInputX, int modelInputY, int modelInputFrames) {
          this.indicesIterator = generateChunkIndicesList().iterator();
          this.imageArr = imageArr;
          this.modelInputX = modelInputX;
          this.modelInputY = modelInputY;
          this.modelInputFrames = modelInputFrames;
      }

      @Override
      public boolean hasNext() {
          return indicesIterator.hasNext();
      }

      @Override
      public Pair<float[][][], ResultIndices> next() {
          if (!hasNext()) {
              throw new NoSuchElementException();
          }

          ChunkIndices chunkIndices = indicesIterator.next();
          ResultIndices resultIndices = mapChunkIndicesToResultIndices(chunkIndices);

          // Extract the chunk
          float[][][] chunk = new float[this.modelInputFrames][this.modelInputX][this.modelInputY];
          for (int x = 0; x < this.modelInputX; x++) {
              for (int y = 0; y < this.modelInputY; y++) {
                  for (int frame = 0; frame < this.modelInputFrames; frame++) {
                      chunk[frame][x][y] = imageArr[chunkIndices.startX + x][chunkIndices.startY + y][chunkIndices.startFrame + frame];
                  }
              }
          }

          return new Pair<>(chunk, resultIndices); // Return chunk and ResultIndices
        }
    }
}
