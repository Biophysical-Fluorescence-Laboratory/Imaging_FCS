package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages spatial partitioning of domains in a two-dimensional space to efficiently
 * query which domain a particle is in. It uses a hashing mechanism to map domains into
 * a grid of cells, facilitating fast lookup operations for spatial queries.
 */
public class DomainHashMap {
    private static final long GRID_DIMENSION_OFFSET = 100000;
    private final double cellSize;
    private final Map<Long, List<Domain>> buckets = new HashMap<>();

    /**
     * Creates a {@code DomainHashMap} with specified cell size for the grid.
     *
     * @param cellSize the size of each grid cell.
     */
    public DomainHashMap(double cellSize) {
        this.cellSize = cellSize;
    }

    /**
     * Computes a hash code for grid cell coordinates based on the provided x and y values.
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     * @return the computed hash code for the grid cell.
     */
    private long hash(double x, double y) {
        long ix = (long) Math.floor(x / cellSize);
        long iy = (long) Math.floor(y / cellSize);

        // The GRID_DIMENSION_OFFSET is used to prevent collision
        return ix + iy * GRID_DIMENSION_OFFSET;
    }

    /**
     * Inserts a domain into the appropriate cell based on its location.
     *
     * @param domain the {@code Domain} to insert.
     */
    public void insert(Domain domain) {
        long bucketId = hash(domain.x, domain.y);
        buckets.computeIfAbsent(bucketId, k -> new ArrayList<>()).add(domain);
    }

    /**
     * Queries for domains in the cell that corresponds to the provided x and y coordinates.
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     * @return a list of {@code Domain} objects in the queried cell.
     */
    public List<Domain> query(double x, double y) {
        long bucketId = hash(x, y);
        return buckets.getOrDefault(bucketId, new ArrayList<>());
    }

    /**
     * Finds a domain in the immediate cell containing the given particle.
     *
     * @param particle the {@code Particle2D} to find the domain for.
     * @return the {@code Domain} containing the particle, or {@code null} if not found.
     */
    private Domain findDomainInImmediateCell(Particle2D particle) {
        for (Domain domain : query(particle.x, particle.y)) {
            if (domain.isParticleInsideDomain(particle)) {
                return domain;
            }
        }

        return null;
    }

    /**
     * Finds a domain in adjacent cells around the given particle's location.
     *
     * @param particle the {@code Particle2D} to find the domain for.
     * @return the {@code Domain} containing the particle, or {@code null} if not found in adjacent cells.
     */
    private Domain findDomainInAdjacentCells(Particle2D particle) {
        // This includes the logic to search in adjacent cells, similar to the expanded query method
        // Calculate the range of cells to check around the particle's position
        int startX = (int) Math.floor((particle.x - cellSize) / cellSize);
        int endX = (int) Math.floor((particle.x + cellSize) / cellSize);
        int startY = (int) Math.floor((particle.y - cellSize) / cellSize);
        int endY = (int) Math.floor((particle.y + cellSize) / cellSize);

        for (int ix = startX; ix <= endX; ix++) {
            for (int iy = startY; iy <= endY; iy++) {
                if (ix == (int) Math.floor(particle.x / cellSize) && iy == (int) Math.floor(particle.y / cellSize)) {
                    continue; // Skip the immediate cell as it's already been checked
                }
                for (Domain domain : query(ix * cellSize, iy * cellSize)) {
                    if (domain.isParticleInsideDomain(particle)) {
                        return domain; // Particle is within this domain
                    }
                }
            }
        }

        return null; // No domain found in adjacent cells
    }

    /**
     * Finds the domain containing the given particle, checking both the immediate and adjacent cells.
     *
     * @param particle the {@code Particle2D} to find the domain for.
     * @return the {@code Domain} containing the particle, or {@code null} if not found.
     */
    public Domain findDomainForParticle(Particle2D particle) {
        Domain domain = findDomainInImmediateCell(particle);
        if (domain == null) {
            domain = findDomainInAdjacentCells(particle);
        }

        return domain;
    }

    /**
     * Checks if a new domain overlaps with any existing domains in the spatial hash map.
     *
     * @param newDomain the {@code Domain} to check for overlap.
     * @return {@code true} if there is an overlap; {@code false} otherwise.
     */
    public boolean hasOverlap(Domain newDomain) {
        // Calculate the range of cells to check based on the domain's radius
        int startX = (int) Math.floor((newDomain.x - newDomain.radius) / cellSize);
        int endX = (int) Math.floor((newDomain.x + newDomain.radius) / cellSize);
        int startY = (int) Math.floor((newDomain.y - newDomain.radius) / cellSize);
        int endY = (int) Math.floor((newDomain.y + newDomain.radius) / cellSize);

        for (int ix = startX; ix <= endX; ix++) {
            for (int iy = startY; iy <= endY; iy++) {
                for (Domain domain : query(ix * cellSize, iy * cellSize)) {
                    double dx = newDomain.x - domain.x;
                    double dy = newDomain.y - domain.y;
                    double distanceSq = dx * dx + dy * dy;
                    if (distanceSq < Math.pow(newDomain.radius + domain.radius, 2)) {
                        return true; // Overlap detected
                    }
                }
            }
        }

        return false; // No overlap found
    }

}
