/*
 * Decompiled with CFR 0.152.
 */
package com.terraforged.noise.func;

public enum EdgeFunc {
    DISTANCE_2{

        @Override
        public float apply(float distance, float distance2) {
            return distance2 - 1.0f;
        }

        @Override
        public float max() {
            return 1.0f;
        }

        @Override
        public float min() {
            return -1.0f;
        }

        @Override
        public float range() {
            return 2.0f;
        }
    }
    ,
    DISTANCE_2_ADD{

        @Override
        public float apply(float distance, float distance2) {
            return distance2 + distance - 1.0f;
        }

        @Override
        public float max() {
            return 1.6f;
        }

        @Override
        public float min() {
            return -1.0f;
        }

        @Override
        public float range() {
            return 2.6f;
        }
    }
    ,
    DISTANCE_2_SUB{

        @Override
        public float apply(float distance, float distance2) {
            return distance2 - distance - 1.0f;
        }

        @Override
        public float max() {
            return 0.8f;
        }

        @Override
        public float min() {
            return -1.0f;
        }

        @Override
        public float range() {
            return 1.8f;
        }
    }
    ,
    DISTANCE_2_MUL{

        @Override
        public float apply(float distance, float distance2) {
            return distance2 * distance - 1.0f;
        }

        @Override
        public float max() {
            return 0.7f;
        }

        @Override
        public float min() {
            return -1.0f;
        }

        @Override
        public float range() {
            return 1.7f;
        }
    }
    ,
    DISTANCE_2_DIV{

        @Override
        public float apply(float distance, float distance2) {
            return distance / distance2 - 1.0f;
        }

        @Override
        public float max() {
            return 0.0f;
        }

        @Override
        public float min() {
            return -1.0f;
        }

        @Override
        public float range() {
            return 1.0f;
        }
    };


    public abstract float apply(float var1, float var2);

    public abstract float max();

    public abstract float min();

    public abstract float range();
}

