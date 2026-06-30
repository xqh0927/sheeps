import { Point3D, TileData } from './types';

export function getDifficultyForLevel(levelId: number): number {
  if (levelId === 1) return 1;
  if (levelId === 2) return 2;
  return 3;
}

export function lcg(seed: number) {
  let s = seed;
  return function () {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

export function generateSolvableLevel(levelId: number, seed: number): TileData[] {
  let coordinates: Point3D[] = [];

  if (levelId === 1) {
    coordinates = [
      { x: 1.0, y: 1.0, z: 0 }, { x: 2.0, y: 1.0, z: 0 },
      { x: 1.0, y: 2.0, z: 0 }, { x: 2.0, y: 2.0, z: 0 },
      { x: 1.5, y: 1.5, z: 1 }, { x: 2.5, y: 1.5, z: 1 },
      { x: 1.5, y: 2.5, z: 1 }, { x: 2.5, y: 2.5, z: 1 },
      { x: 2.0, y: 2.0, z: 2 },
      { x: 2.0, y: 1.0, z: 3 }, { x: 1.0, y: 2.0, z: 3 }, { x: 2.0, y: 2.0, z: 3 }
    ];
  } else {
    // Single player or multiplayer cards strictly incremental: 24 + levelId * 12
    const maxCards = 24 + levelId * 12;
    const possibleCoords: Point3D[] = [];

    // Layers count L = 12 - 8 / sqrt(x - 1), capped at 12
    const layersCount = Math.min(12, Math.floor(12 - 8 / Math.sqrt(levelId - 1)));

    const baseSize = 6 + Math.floor(levelId / 20);

    // Choose one of the 18 shapes based on seed
    const shapeType = seed % 18;

    for (let z = 0; z < layersCount; z++) {
      const size = Math.max(3, baseSize - Math.floor(z / 3));
      const offset = (z % 2 === 0) ? 0 : 0.5;
      const center = (size - 1) / 2;

      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
          let keep = true;

          // Normalized offsets from center (-1.0 to 1.0)
          const dx = center > 0 ? (c - center) / center : 0;
          const dy = center > 0 ? (r - center) / center : 0;
          const distMan = Math.abs(dx) + Math.abs(dy);
          const distEuclid = Math.sqrt(dx * dx + dy * dy);

          switch (shapeType) {
            case 0: // Square
              keep = true;
              break;
            case 1: // Pyramid / Triangle
              const margin = z * 0.45;
              if (r < margin || r >= size - margin || c < margin || c >= size - margin) {
                keep = false;
              }
              break;
            case 2: // Cross
              if (Math.abs(c - center) >= 1.2 && Math.abs(r - center) >= 1.2) {
                keep = false;
              }
              break;
            case 3: // Diamond
              if (distMan > 1.15) {
                keep = false;
              }
              break;
            case 4: // Ring
              if (distEuclid < 0.4 || distEuclid > 1.05) {
                keep = false;
              }
              break;
            case 5: // X-Shape
              if (Math.abs(Math.abs(dx) - Math.abs(dy)) > 0.28) {
                keep = false;
              }
              break;
            case 6: // Heart
              if (dx * dx + (dy - Math.abs(dx) * 0.6) * (dy - Math.abs(dx) * 0.6) > 0.95) {
                keep = false;
              }
              break;
            case 7: // Hourglass
              if (Math.abs(dx) > Math.abs(dy) + 0.1) {
                keep = false;
              }
              break;
            case 8: // Star
              if (Math.abs(dx) >= 0.22 && Math.abs(dy) >= 0.22 && Math.abs(Math.abs(dx) - Math.abs(dy)) >= 0.22) {
                keep = false;
              }
              break;
            case 9: // Hollow Square
              if (Math.abs(dx) <= 0.6 && Math.abs(dy) <= 0.6) {
                keep = false;
              }
              break;
            case 10: // Double Ring
              if (Math.abs(distEuclid - 0.75) > 0.25 && Math.abs(distEuclid - 0.3) > 0.2) {
                keep = false;
              }
              break;
            case 11: // Grid Hollow
              if ((r + c) % 2 !== 0) {
                keep = false;
              }
              break;
            case 12: // Arrow
              if (dy < -0.7 || Math.abs(dx) > (dy + 1.0) * 0.8) {
                keep = false;
              }
              break;
            case 13: // Butterfly
              if (Math.abs(dx) < Math.abs(dy) * 0.65) {
                keep = false;
              }
              break;
            case 14: // Concentric Circles
              if (distEuclid >= 0.35 && (distEuclid <= 0.68 || distEuclid >= 1.0)) {
                keep = false;
              }
              break;
            case 15: // Tai Chi
              if (distEuclid >= 1.02 || (dy <= Math.sin(dx * Math.PI) * 0.45)) {
                keep = false;
              }
              break;
            case 16: // Staircase
              if (r + c < size / 2 || r + c >= size * 1.5) {
                keep = false;
              }
              break;
            case 17: // Hexagon
              if (Math.abs(dy) > 0.95 || Math.abs(dx) * 1.5 + Math.abs(dy) > 1.55) {
                keep = false;
              }
              break;
          }

          if (keep) {
            possibleCoords.push({
              x: c + offset + 1.0,
              y: r + offset + 1.0,
              z: z
            });
          }
        }
      }
    }

    let rand = lcg(seed);
    for (let i = possibleCoords.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      const temp = possibleCoords[i];
      possibleCoords[i] = possibleCoords[j];
      possibleCoords[j] = temp;
    }

    const count = Math.min(possibleCoords.length, maxCards) - (Math.min(possibleCoords.length, maxCards) % 3);
    coordinates = possibleCoords.slice(0, count);
  }

  coordinates.sort((a, b) => a.z - b.z);

  const W = 1.0;
  const H = 1.0;

  // Card types T increases logarithmically: T = 3 + 3 * ln(x), capped at 16
  const numTypes = levelId === 1 ? 3 : Math.min(16, Math.floor(3 + 3 * Math.log(levelId)));

  interface Node {
    index: number;
    coord: Point3D;
    assignedType: number;
  }

  const nodes: Node[] = coordinates.map((c, idx) => ({
    index: idx,
    coord: c,
    assignedType: -1
  }));

  const blocks = (a: Point3D, b: Point3D) => {
    return a.z > b.z && Math.abs(a.x - b.x) < W && Math.abs(a.y - b.y) < H;
  };

  const unassigned = new Set<Node>(nodes);
  let randAssign = lcg(seed + 100);

  while (unassigned.size > 0) {
    const exposedNodes: Node[] = [];
    for (const node of unassigned) {
      let isCovered = false;
      for (const other of unassigned) {
        if (other !== node && blocks(other.coord, node.coord)) {
          isCovered = true;
          break;
        }
      }
      if (!isCovered) {
        exposedNodes.push(node);
      }
    }

    if (exposedNodes.length < 3) {
      const rem = Array.from(unassigned);
      while (rem.length >= 3) {
        const type = Math.floor(randAssign() * numTypes) + 1;
        for (let k = 0; k < 3; k++) {
          const n = rem.pop()!;
          n.assignedType = type;
          unassigned.delete(n);
        }
      }
      for (const n of rem) {
        n.assignedType = 1;
        unassigned.delete(n);
      }
      break;
    }

    const type = Math.floor(randAssign() * numTypes) + 1;
    for (let k = 0; k < 3; k++) {
      const idx = Math.floor(randAssign() * exposedNodes.length);
      const chosen = exposedNodes.splice(idx, 1)[0];
      chosen.assignedType = type;
      unassigned.delete(chosen);
    }
  }

  let randProps = lcg(seed + 200);
  return nodes.map((node) => {
    let isBlind = false;
    let sealedCount = 0;

    if (levelId >= 2) {
      const r = randProps();
      if (levelId % 10 === 0 && r < 0.15) {
        isBlind = true;
      } else if (r < 0.30) {
        sealedCount = 1;
      }
    }

    return {
      id: `tile_${node.index}`,
      x: node.coord.x,
      y: node.coord.y,
      z: node.coord.z,
      type: node.assignedType,
      isBlind,
      sealedCount
    };
  });
}
