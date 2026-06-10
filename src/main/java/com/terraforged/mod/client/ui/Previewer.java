package com.terraforged.mod.client.ui;

import com.terraforged.mod.worldgen.noise.continent.ContinentPreview;
import com.terraforged.noise.Module;
import com.terraforged.noise.util.NoiseUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Previewer
extends JPanel {
    private final Supplier<Shader> shaderSupplier;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Shader shader;
    private int lastX = 0;
    private int lastY = 0;
    private float posX;
    private float posY;
    private float zoom = 1.0f;

    public Previewer(Supplier<Shader> shaderSupplier) {
        this.shaderSupplier = shaderSupplier;
        this.shader = shaderSupplier.get();
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                Previewer.this.lastX = e.getX();
                Previewer.this.lastY = e.getY();
            }
        });
        this.addMouseMotionListener(new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent e) {
                Previewer.this.posX += (float)(Previewer.this.lastX - e.getX()) * Previewer.this.zoom;
                Previewer.this.posY += (float)(Previewer.this.lastY - e.getY()) * Previewer.this.zoom;
                Previewer.this.lastX = e.getX();
                Previewer.this.lastY = e.getY();
                Previewer.this.redraw();
            }
        });
        this.addMouseWheelListener(new MouseAdapter(){

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Previewer.this.zoom = Math.max(1.0E-4f, Previewer.this.zoom + (float)e.getWheelRotation() * 0.75f);
                Previewer.this.redraw();
            }
        });
        this.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == 'r') {
                    Previewer.this.shader = Previewer.this.shaderSupplier.get();
                    Previewer.this.redraw();
                }
                if (e.getKeyChar() == 't') {
                    Previewer.this.zoom = 1.0f;
                    Previewer.this.redraw();
                }
                if (e.getKeyChar() == 's') {
                    ContinentPreview.SEED = ThreadLocalRandom.current().nextInt();
                    Previewer.this.shader = Previewer.this.shaderSupplier.get();
                    Previewer.this.redraw();
                }
            }
        });
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(400, 400));
    }

    protected void redraw() {
        this.repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int w = this.getWidth();
        int h = this.getHeight();
        float ox = this.posX - (float)(w >> 1) * this.zoom;
        float oy = this.posY - (float)(h >> 1) * this.zoom;
        BufferedImage image = new BufferedImage(w, h, 1);
        this.each(ox, oy, this.zoom, 0, 0, w, h, this.shader, image).join();
        g.drawImage(image, 0, 0, w, h, null);
        int width = NoiseUtil.floor((float)w * this.zoom);
        int height = NoiseUtil.floor((float)h * this.zoom);
        g.setColor(Color.WHITE);
        g.drawString(String.format("%sx%s", width, height), 2, 12);
    }

    public static void display(int seed, Supplier<Module> supplier) {
        Previewer.launch(() -> {
            Module noise = (Module)supplier.get();
            return (x, y) -> Color.HSBtoRGB(0.0f, 0.0f, noise.getValue(x, y));
        });
    }

    public static void launch(Supplier<Shader> shader) {
        JFrame frame = new JFrame();
        frame.add(new Previewer(shader));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }

    private <T> CompletableFuture<Void> each(float ox, float oy, float zoom, int x0, int y0, int x1, int y1, Shader shader, BufferedImage image) {
        int tileDivs = (int)Math.floor(Math.sqrt(Runtime.getRuntime().availableProcessors()));
        int tileSizeX = Worker.getTileSize(x1 - x0, tileDivs);
        int tileSizeY = Worker.getTileSize(y1 - y0, tileDivs);
        CompletableFuture[] tasks = new CompletableFuture[tileDivs * tileDivs];
        for (int ty = 0; ty < tileDivs; ++ty) {
            int minY = y0 + ty * tileSizeY;
            int maxY = Math.min(minY + tileSizeY, y1);
            for (int tx = 0; tx < tileDivs; ++tx) {
                int minX = x0 + tx * tileSizeX;
                int maxX = Math.min(minX + tileSizeX, x1);
                Worker worker = new Worker(ox, oy, zoom, minX, minY, maxX, maxY, shader, image);
                tasks[ty * tileDivs + tx] = CompletableFuture.runAsync(worker, this.executor);
            }
        }
        return CompletableFuture.allOf(tasks);
    }

    public static interface Shader {
        public int getRGB(float var1, float var2);
    }

    private static class Worker<T>
    implements Runnable {
        private final float ox;
        private final float oy;
        private final float zoom;
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final Shader shader;
        private final BufferedImage image;

        private Worker(float ox, float oy, float zoom, int minX, int minY, int maxX, int maxY, Shader shader, BufferedImage image) {
            this.ox = ox;
            this.oy = oy;
            this.zoom = zoom;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.shader = shader;
            this.image = image;
        }

        @Override
        public void run() {
            for (int y = this.minY; y < this.maxY; ++y) {
                float py = this.oy + (float)y * this.zoom;
                for (int x = this.minX; x < this.maxX; ++x) {
                    float px = this.ox + (float)x * this.zoom;
                    int rgb = this.shader.getRGB(px, py);
                    this.image.setRGB(x, y, rgb);
                }
            }
        }

        protected static int getTileSize(int dimension, int divs) {
            int size = dimension / divs;
            if (size * divs < dimension) {
                ++size;
            }
            return size;
        }
    }

    private static interface Visitor<T> {
        public void visit(int var1, int var2, float var3, T var4);
    }
}
