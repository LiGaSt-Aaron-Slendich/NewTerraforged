package com.terraforged.mod.util.storage;

public interface Index {
    public static final Index CHUNK = (x, z) -> (z & 0xF) << 4 | x & 0xF;

    public int of(int var1, int var2);

    public static Index borderedChunk(final int border) {
        return new Index(){
            private final int offset;
            private final int size;
            {
                this.offset = border;
                this.size = 16 + border * 2;
            }

            @Override
            public int of(int x, int z) {
                return (z += this.offset) * this.size + (x += this.offset);
            }
        };
    }
}
