package com.terraforged.mod.util.ui;

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

public class Previewer extends JPanel {
   private final Supplier<Previewer.Shader> shaderSupplier;
   private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
   private Previewer.Shader shader;
   private int lastX = 0;
   private int lastY = 0;
   private float posX;
   private float posY;
   private float zoom = 1.0F;

   public Previewer(Supplier<Previewer.Shader> shaderSupplier) {
      this.shaderSupplier = shaderSupplier;
      this.shader = shaderSupplier.get();
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            Previewer.this.lastX = e.getX();
            Previewer.this.lastY = e.getY();
         }
      });
      this.addMouseMotionListener(new MouseAdapter() {
         @Override
         public void mouseDragged(MouseEvent e) {
            Previewer.this.posX = Previewer.this.posX + (Previewer.this.lastX - e.getX()) * Previewer.this.zoom;
            Previewer.this.posY = Previewer.this.posY + (Previewer.this.lastY - e.getY()) * Previewer.this.zoom;
            Previewer.this.lastX = e.getX();
            Previewer.this.lastY = e.getY();
            Previewer.this.redraw();
         }
      });
      this.addMouseWheelListener(new MouseAdapter() {
         @Override
         public void mouseWheelMoved(MouseWheelEvent e) {
            Previewer.this.zoom = Math.max(1.0E-4F, Previewer.this.zoom + e.getWheelRotation() * 0.75F);
            Previewer.this.redraw();
         }
      });
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyChar() == 'r') {
               Previewer.this.shader = Previewer.this.shaderSupplier.get();
               Previewer.this.redraw();
            }

            if (e.getKeyChar() == 't') {
               Previewer.this.zoom = 1.0F;
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
      int i = this.getWidth();
      int j = this.getHeight();
      float f = this.posX - (i >> 1) * this.zoom;
      float f1 = this.posY - (j >> 1) * this.zoom;
      BufferedImage bufferedimage = new BufferedImage(i, j, 1);
      this.each(f, f1, this.zoom, 0, 0, i, j, this.shader, bufferedimage).join();
      g.drawImage(bufferedimage, 0, 0, i, j, null);
      int k = NoiseUtil.floor(i * this.zoom);
      int l = NoiseUtil.floor(j * this.zoom);
      g.setColor(Color.WHITE);
      g.drawString(String.format("%sx%s", k, l), 2, 12);
   }

   public static void display(Supplier<Module> supplier) {
      launch(() -> {
         Module module = supplier.get();
         return (x, y) -> Color.HSBtoRGB(0.0F, 0.0F, module.getValue(x, y));
      });
   }

   public static void launch(Supplier<Previewer.Shader> shader) {
      JFrame jframe = new JFrame();
      jframe.add(new Previewer(shader));
      jframe.pack();
      jframe.setLocationRelativeTo(null);
      jframe.setDefaultCloseOperation(3);
      jframe.setVisible(true);
   }

   private <T> CompletableFuture<Void> each(float ox, float oy, float zoom, int x0, int y0, int x1, int y1, Previewer.Shader shader, BufferedImage image) {
      int i = (int)Math.floor(Math.sqrt(Runtime.getRuntime().availableProcessors()));
      int j = Previewer.Worker.getTileSize(x1 - x0, i);
      int k = Previewer.Worker.getTileSize(y1 - y0, i);
      CompletableFuture[] acompletablefuture = new CompletableFuture[i * i];

      for (int l = 0; l < i; l++) {
         int i1 = y0 + l * k;
         int j1 = Math.min(i1 + k, y1);

         for (int k1 = 0; k1 < i; k1++) {
            int l1 = x0 + k1 * j;
            int i2 = Math.min(l1 + j, x1);
            Previewer.Worker<Object> worker = new Previewer.Worker<>(ox, oy, zoom, l1, i1, i2, j1, shader, image);
            acompletablefuture[l * i + k1] = CompletableFuture.runAsync(worker, this.executor);
         }
      }

      return CompletableFuture.allOf(acompletablefuture);
   }

   public interface Shader {
      int getRGB(float var1, float var2);
   }

   private interface Visitor<T> {
      void visit(int var1, int var2, float var3, T var4);
   }

   private static class Worker<T> implements Runnable {
      private final float ox;
      private final float oy;
      private final float zoom;
      private final int minX;
      private final int minY;
      private final int maxX;
      private final int maxY;
      private final Previewer.Shader shader;
      private final BufferedImage image;

      private Worker(float ox, float oy, float zoom, int minX, int minY, int maxX, int maxY, Previewer.Shader shader, BufferedImage image) {
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
         for (int i = this.minY; i < this.maxY; i++) {
            float f = this.oy + i * this.zoom;

            for (int j = this.minX; j < this.maxX; j++) {
               float f1 = this.ox + j * this.zoom;
               int k = this.shader.getRGB(f1, f);
               this.image.setRGB(j, i, k);
            }
         }
      }

      protected static int getTileSize(int dimension, int divs) {
         int i = dimension / divs;
         if (i * divs < dimension) {
            i++;
         }

         return i;
      }
   }
}
