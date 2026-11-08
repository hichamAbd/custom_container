package com.reelriot.vegas.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.MathUtils;
import com.reelriot.vegas.gfx.Figure;
import com.reelriot.vegas.gfx.Mat;
import com.reelriot.vegas.gfx.Part;

import static com.reelriot.vegas.gfx.Part.ball;
import static com.reelriot.vegas.gfx.Part.box;
import static com.reelriot.vegas.gfx.Part.cone;
import static com.reelriot.vegas.gfx.Part.cyl;
import static com.reelriot.vegas.gfx.Part.gem;
import static com.reelriot.vegas.gfx.Part.group;
import static com.reelriot.vegas.gfx.Part.ring;

/** The four hosts, built as glossy 3D figurines. Each stands on y = 0. */
public final class Hosts {
    private Hosts() { }

    public static Figure make(String id) {
        switch (id) {
            case "vivi": return new Vivi();
            case "bruno": return new Bruno();
            case "goldbeak": return new Goldbeak();
            default: return new Mochi();
        }
    }

    static Color c(float r, float g, float b) { return new Color(r, g, b, 1); }

    static final Color INK = c(0.04f, 0.02f, 0.03f);

    /** A glossy eye with a highlight; returns the eye part (scale its y to blink). */
    static Part eye(Part head, float x, float y, float z, float w, float h, Color iris) {
        Part e = head.add(ball(Mat.lacquer(iris)).at(x, y, z).sized(w, h, w * 0.6f));
        e.add(ball(Mat.lit(Color.WHITE, 1.4f)).at(w * 0.22f, h * 0.22f, w * 0.26f).sized(w * 0.36f));
        return e;
    }

    /* ------------------------------------------------------------- Mochi */

    /** Mochi, the golden lucky cat of Lucky Paws Palace. */
    public static class Mochi extends Figure {
        final Part body, head, armR, paw, coin, eyeL, eyeR, mouth, tail;

        Mochi() {
            Material gold = Mat.gold(), cream = Mat.satin(Mat.CREAM), pink = Mat.glossy(c(1f, 0.55f, 0.65f));
            body = root.add(ball(gold).at(0, 0.62f, 0).sized(1.1f, 1.0f, 0.95f));
            root.add(ball(cream).at(0, 0.55f, 0.3f).sized(0.72f, 0.72f, 0.46f));
            for (int s = -1; s <= 1; s += 2) root.add(ball(cream).at(s * 0.27f, 0.08f, 0.3f).sized(0.36f, 0.2f, 0.42f));
            tail = root.add(group().at(0.42f, 0.35f, -0.32f));
            tail.add(ball(gold).at(0.08f, 0, 0).sized(0.24f));
            tail.add(ball(gold).at(0.2f, 0.2f, -0.04f).sized(0.22f));
            tail.add(ball(gold).at(0.22f, 0.42f, -0.02f).sized(0.2f));
            root.add(ring(Mat.glossy(Mat.RUBY), 0.22f).at(0, 1.05f, 0).sized(0.86f, 0.9f, 0.82f));
            root.add(ball(gold).at(0, 0.98f, 0.42f).sized(0.17f));
            coin = root.add(cyl(Mat.gold()).at(-0.16f, 0.62f, 0.52f).turned(0, 70, 0).sized(0.36f, 0.07f, 0.46f));
            root.add(ball(cream).at(0.08f, 0.66f, 0.62f).sized(0.22f, 0.2f, 0.2f));
            armR = root.add(group().at(0.48f, 0.92f, 0.08f).turned(0, 0, -18));
            armR.add(ball(gold).at(0.04f, 0.24f, 0).sized(0.27f, 0.52f, 0.27f));
            paw = armR.add(ball(cream).at(0.06f, 0.52f, 0.02f).sized(0.3f));
            paw.add(ball(pink).at(0, -0.02f, 0.13f).sized(0.12f, 0.1f, 0.05f));
            for (int i = -1; i <= 1; i++) paw.add(ball(pink).at(i * 0.07f, 0.07f, 0.12f).sized(0.05f));
            head = root.add(group().at(0, 1.45f, 0));
            head.add(ball(gold).sized(1.0f, 0.88f, 0.86f));
            for (int s = -1; s <= 1; s += 2) {
                head.add(cone(gold).at(s * 0.3f, 0.42f, -0.02f).turned(0, 0, -s * 20).sized(0.32f, 0.38f, 0.24f));
                head.add(cone(pink).at(s * 0.3f, 0.4f, 0.07f).turned(0, 0, -s * 20).sized(0.18f, 0.26f, 0.06f));
                head.add(ball(Mat.satin(c(1f, 0.6f, 0.68f))).at(s * 0.3f, -0.12f, 0.3f).sized(0.16f, 0.1f, 0.06f));
                for (int w = -1; w <= 1; w += 2) head.add(cyl(Mat.satin(INK)).at(s * 0.4f, -0.1f + w * 0.035f, 0.26f).turned(0, 0, s * (90 + w * 8)).sized(0.012f, 0.3f, 0.012f));
            }
            head.add(ball(cream).at(0, -0.14f, 0.36f).sized(0.36f, 0.24f, 0.2f));
            head.add(ball(pink).at(0, -0.07f, 0.46f).sized(0.09f, 0.06f, 0.06f));
            mouth = head.add(ball(Mat.glossy(c(0.45f, 0.05f, 0.1f))).at(0, -0.2f, 0.43f).sized(0.08f, 0.03f, 0.03f));
            eyeL = eye(head, -0.18f, 0.06f, 0.38f, 0.13f, 0.17f, INK);
            eyeR = eye(head, 0.18f, 0.06f, 0.38f, 0.13f, 0.17f, INK);
            head.add(gem(Mat.jewel(Mat.RUBY)).at(0, 0.3f, 0.4f).sized(0.1f, 0.12f, 0.07f));
        }

        @Override
        protected void pose(float t) {
            float wave = 12, speed = 3, jump = 0;
            switch (mood) {
                case TALK: case WAVE: wave = 28; speed = 8; break;
                case CHEER: wave = 40; speed = 10; jump = hop(t, 7, 0.28f); break;
                case WOW: wave = 0; jump = hop(moodT, 9, 0.12f); break;
                case SAD: wave = 4; speed = 1.5f; break;
                default: break;
            }
            root.offset.y = jump + MathUtils.sin(t * 2.2f) * 0.02f;
            armR.spin(0, 0, 1, -wave * (0.5f + 0.5f * MathUtils.sin(t * speed)));
            if (mood == Mood.CHEER) coin.offset.y = 0.25f + hop(t, 7, 0.15f);
            head.spin(0, 0, 1, MathUtils.sin(t * 0.9f) * 4);
            if (mood == Mood.SAD) head.spin(1, 0, 0, 14);
            if (mood == Mood.TALK) mouth.size.y = 0.03f + 0.05f * Math.abs(MathUtils.sin(t * 14));
            if (mood == Mood.CHEER || mood == Mood.WOW) mouth.size.set(0.11f, 0.08f, 0.04f);
            float b = blink(t, 3.6f, 0);
            float wow = mood == Mood.WOW ? 1.25f : 1f;
            eyeL.size.y *= (1 - b * 0.9f) * wow;
            eyeR.size.y *= (1 - b * 0.9f) * wow;
            tail.spin(0, 0, 1, MathUtils.sin(t * 2.6f) * 10);
        }
    }

    /* -------------------------------------------------------------- Vivi */

    /** Vivi Velvet, the casino hostess. */
    public static class Vivi extends Figure {
        final Part head, armL, armR, plumes, eyeL, eyeR, lips;

        Vivi() {
            Material skin = Mat.satin(c(0.9f, 0.62f, 0.46f)), hair = Mat.glossy(c(0.28f, 0.04f, 0.12f));
            Material dress = Mat.make(c(0.9f, 0.08f, 0.2f), 0.55f, 0.22f), glove = Mat.satin(c(1f, 0.96f, 0.98f));
            Material gold = Mat.gold();
            for (int s = -1; s <= 1; s += 2) {
                root.add(cyl(skin).at(s * 0.1f, 0.26f, 0).sized(0.11f, 0.5f, 0.11f));
                root.add(ball(gold).at(s * 0.1f, 0.04f, 0.05f).sized(0.15f, 0.09f, 0.24f));
            }
            root.add(cone(dress).at(0, 0.66f, 0).sized(0.8f, 0.84f, 0.66f));
            root.add(ball(dress).at(0, 1.02f, 0).sized(0.44f, 0.52f, 0.34f));
            root.add(ring(Mat.lacquer(c(1f, 0.98f, 0.92f)), 0.3f).at(0, 1.24f, 0.02f).sized(0.27f, 0.3f, 0.25f));
            root.add(cyl(skin).at(0, 1.3f, 0).sized(0.1f, 0.14f, 0.1f));
            armL = root.add(group().at(-0.25f, 1.18f, 0).turned(0, 0, 14));
            armL.add(cyl(glove).at(0, -0.22f, 0).sized(0.09f, 0.44f, 0.09f));
            armL.add(ball(glove).at(0, -0.46f, 0).sized(0.12f));
            armR = root.add(group().at(0.25f, 1.18f, 0).turned(0, 0, 118));
            armR.add(cyl(glove).at(0, -0.22f, 0).sized(0.09f, 0.44f, 0.09f));
            armR.add(ball(glove).at(0, -0.46f, 0).sized(0.13f));
            head = root.add(group().at(0, 1.58f, 0));
            head.add(ball(skin).sized(0.56f, 0.58f, 0.52f));
            head.add(ball(hair).at(0, 0.05f, -0.07f).sized(0.67f, 0.67f, 0.6f));
            for (int s = -1; s <= 1; s += 2) {
                head.add(ball(hair).at(s * 0.25f, -0.14f, -0.02f).sized(0.24f, 0.44f, 0.32f));
                head.add(ball(Mat.satin(c(1f, 0.5f, 0.6f))).at(s * 0.15f, -0.08f, 0.22f).sized(0.1f, 0.06f, 0.04f));
                head.add(ball(gold).at(s * 0.28f, -0.13f, 0.02f).sized(0.06f));
                head.add(gem(Mat.jewel(Mat.RUBY)).at(s * 0.28f, -0.22f, 0.02f).sized(0.05f, 0.08f, 0.05f));
            }
            head.add(ball(hair).at(0, 0.2f, 0.13f).turned(0, -12, 0).sized(0.56f, 0.24f, 0.36f));
            eyeL = eye(head, -0.1f, 0.0f, 0.23f, 0.075f, 0.1f, c(0.1f, 0.03f, 0.06f));
            eyeR = eye(head, 0.1f, 0.0f, 0.23f, 0.075f, 0.1f, c(0.1f, 0.03f, 0.06f));
            lips = head.add(ball(Mat.glossy(c(0.85f, 0.05f, 0.2f))).at(0, -0.14f, 0.24f).sized(0.1f, 0.045f, 0.05f));
            head.add(ring(gold, 0.14f).at(0, 0.24f, 0.03f).turned(0, -22, 0).sized(0.5f, 0.5f, 0.48f));
            head.add(gem(Mat.jewel(Mat.RUBY)).at(0, 0.34f, 0.2f).sized(0.09f, 0.11f, 0.06f));
            plumes = head.add(group().at(0, 0.28f, -0.14f));
            Material pink = Mat.satin(c(1f, 0.42f, 0.78f)), white = Mat.satin(c(1f, 0.92f, 0.98f));
            float[] ang = { -44, -22, 0, 22, 44 };
            for (int i = 0; i < ang.length; i++) {
                Part f = plumes.add(group().turned(0, 0, ang[i]));
                f.add(ball(i % 2 == 0 ? pink : white).at(0, 0.36f, 0).sized(0.15f, 0.62f, 0.05f));
            }
            for (int s = -1; s <= 1; s += 2) plumes.add(group().turned(0, 0, s * 33)).add(ball(gold).at(0, 0.3f, -0.02f).sized(0.08f, 0.5f, 0.03f));
        }

        @Override
        protected void pose(float t) {
            float sway = MathUtils.sin(t * 1.4f);
            root.offset.y = MathUtils.sin(t * 2.8f) * 0.012f;
            head.spin(0, 0, 1, sway * 3);
            plumes.spin(0, 0, 1, MathUtils.sin(t * 2.1f) * 4);
            armR.spin(0, 0, 1, MathUtils.sin(t * 2.4f) * 6);
            switch (mood) {
                case TALK:
                    lips.size.y = 0.04f + 0.05f * Math.abs(MathUtils.sin(t * 13));
                    head.spin(1, 0, 0, MathUtils.sin(t * 5) * 4);
                    break;
                case WAVE:
                    armR.spin(0, 0, 1, 20 + MathUtils.sin(t * 9) * 22);
                    break;
                case CHEER:
                    root.offset.y += hop(t, 6.5f, 0.18f);
                    armL.spin(0, 0, 1, -120 + MathUtils.sin(t * 13) * 12);
                    armR.spin(0, 0, 1, 18 + MathUtils.sin(t * 13) * 12);
                    lips.size.set(0.11f, 0.08f, 0.05f);
                    break;
                case WOW:
                    armL.spin(0, 0, 1, -70);
                    lips.size.set(0.07f, 0.07f, 0.05f);
                    break;
                case SAD:
                    head.spin(1, 0, 0, 12);
                    break;
                default:
                    break;
            }
            float b = blink(t, 3.9f, 0.3f);
            eyeL.size.y *= 1 - b * 0.9f;
            // a wink every few seconds
            float w = blink(t, 5.3f, 1.7f);
            eyeR.size.y *= 1 - Math.max(b, w) * 0.9f;
        }
    }

    /* ------------------------------------------------------------- Bruno */

    /** Bruno "Big Bucks", the bulldog high roller. */
    public static class Bruno extends Figure {
        final Part head, browL, browR, chips, armR, hat;
        final Part[] chip = new Part[4];

        Bruno() {
            Material fawn = Mat.satin(c(0.88f, 0.6f, 0.34f)), cream = Mat.satin(c(1f, 0.88f, 0.72f));
            Material suit = Mat.make(c(0.42f, 0.13f, 0.74f), 0f, 0.62f), gold = Mat.gold();
            Material brown = Mat.satin(c(0.36f, 0.18f, 0.08f)), black = Mat.lacquer(INK);
            for (int s = -1; s <= 1; s += 2) {
                root.add(cyl(suit).at(s * 0.18f, 0.2f, 0).sized(0.22f, 0.4f, 0.22f));
                root.add(ball(black).at(s * 0.18f, 0.03f, 0.08f).sized(0.24f, 0.1f, 0.34f));
            }
            root.add(ball(suit).at(0, 0.7f, 0).sized(0.94f, 0.88f, 0.74f));
            root.add(ball(Mat.satin(Color.WHITE)).at(0, 0.8f, 0.24f).sized(0.32f, 0.5f, 0.3f));
            for (int s = -1; s <= 1; s += 2) {
                root.add(box(gold).at(s * 0.13f, 0.8f, 0.3f).turned(0, 0, s * 16).sized(0.08f, 0.5f, 0.04f));
                root.add(cone(Mat.glossy(Mat.RUBY)).at(s * 0.08f, 1.03f, 0.33f).turned(0, 0, s * 90).sized(0.12f, 0.14f, 0.08f));
            }
            root.add(ball(Mat.glossy(Mat.RUBY)).at(0, 1.03f, 0.35f).sized(0.06f));
            root.add(ring(gold, 0.1f).at(0, 0.82f, 0.16f).turned(0, 72, 0).sized(0.62f, 0.6f, 0.6f));
            root.add(cyl(gold).at(0, 0.6f, 0.37f).turned(0, 90, 0).sized(0.15f, 0.03f, 0.15f));
            Part armL = root.add(group().at(-0.45f, 0.92f, 0).turned(0, 0, 10));
            armL.add(cyl(suit).at(0, -0.2f, 0.04f).sized(0.2f, 0.4f, 0.2f));
            armL.add(ball(fawn).at(0, -0.42f, 0.08f).sized(0.2f));
            armR = root.add(group().at(0.45f, 0.9f, 0.02f).turned(0, -55, 0));
            armR.add(cyl(suit).at(0, -0.18f, 0).sized(0.2f, 0.38f, 0.2f));
            armR.add(ball(fawn).at(0, -0.4f, 0).sized(0.21f));
            chips = root.add(group().at(0.5f, 0.82f, 0.34f));
            for (int i = 0; i < 4; i++) {
                chip[i] = chips.add(cyl(Mat.glossy(i % 2 == 0 ? Mat.RUBY : c(0.1f, 0.1f, 0.12f))).at(0, i * 0.055f, 0).sized(0.22f, 0.045f, 0.22f));
                chip[i].add(ring(Mat.satin(Color.WHITE), 0.18f).sized(0.2f, 0.4f, 0.2f));
            }
            head = root.add(group().at(0, 1.27f, 0.05f));
            head.add(ball(fawn).sized(0.96f, 0.76f, 0.82f));
            head.add(ball(cream).at(0, -0.14f, 0.33f).sized(0.54f, 0.34f, 0.32f));
            for (int s = -1; s <= 1; s += 2) {
                head.add(ball(cream).at(s * 0.15f, -0.2f, 0.38f).sized(0.22f, 0.2f, 0.18f));
                head.add(ball(brown).at(s * 0.43f, 0.16f, -0.02f).turned(0, 0, s * 32).sized(0.16f, 0.32f, 0.26f));
                head.add(cone(Mat.glossy(Color.WHITE)).at(s * 0.08f, -0.24f, 0.47f).sized(0.05f, 0.08f, 0.05f));
                eye(head, s * 0.2f, 0.07f, 0.35f, 0.1f, 0.11f, INK);
            }
            head.add(ball(black).at(0, -0.03f, 0.47f).sized(0.21f, 0.12f, 0.1f));
            head.add(ball(Mat.satin(c(0.8f, 0.42f, 0.36f))).at(0, -0.3f, 0.36f).sized(0.36f, 0.14f, 0.2f));
            browL = head.add(box(brown).at(-0.2f, 0.19f, 0.37f).turned(0, 0, 8).sized(0.18f, 0.04f, 0.04f));
            browR = head.add(box(brown).at(0.2f, 0.21f, 0.37f).turned(0, 0, -14).sized(0.18f, 0.04f, 0.04f));
            hat = head.add(group().at(0, 0.36f, -0.02f).turned(0, 0, -8));
            Material felt = Mat.glossy(c(0.2f, 0.06f, 0.36f));
            hat.add(cyl(felt).sized(1.0f, 0.05f, 0.9f));
            hat.add(cyl(felt).at(0, 0.17f, 0).sized(0.62f, 0.32f, 0.56f));
            hat.add(cyl(gold).at(0, 0.07f, 0).sized(0.64f, 0.08f, 0.58f));
        }

        @Override
        protected void pose(float t) {
            root.offset.y = MathUtils.sin(t * 1.8f) * 0.015f;
            head.spin(0, 1, 0, MathUtils.sin(t * 0.7f) * 8);
            float k = (t % 2.4f) / 2.4f;
            // shuffle: the top chip flips over
            chip[3].offset.y = k < 0.25f ? MathUtils.sin(k / 0.25f * MathUtils.PI) * 0.18f : 0;
            chip[3].spin(1, 0, 0, k < 0.25f ? k / 0.25f * 360 : 0);
            float raise = (t % 5f) < 0.6f ? 0.04f : 0;
            browR.offset.y = raise;
            switch (mood) {
                case TALK:
                    head.spin(1, 0, 0, MathUtils.sin(t * 9) * 4);
                    break;
                case CHEER:
                    root.offset.y += hop(t, 6, 0.2f);
                    chips.offset.y = hop(t, 6, 0.5f);
                    for (int i = 0; i < 4; i++) chip[i].spin(1, 0, 1, t * (400 + i * 90));
                    browL.offset.y = browR.offset.y = 0.05f;
                    break;
                case WOW:
                    browL.offset.y = browR.offset.y = 0.06f;
                    root.offset.y += hop(moodT, 8, 0.08f);
                    break;
                case SAD:
                    browL.spin(0, 0, 1, -16);
                    browR.spin(0, 0, 1, 20);
                    head.spin(1, 0, 0, 10);
                    break;
                default:
                    break;
            }
        }
    }

    /* ---------------------------------------------------------- Goldbeak */

    /** Captain Goldbeak, the parrot pirate. */
    public static class Goldbeak extends Figure {
        final Part head, wingL, wingR, jaw, eye;

        Goldbeak() {
            Material red = Mat.glossy(c(0.88f, 0.1f, 0.13f)), blue = Mat.glossy(c(0.1f, 0.36f, 0.9f));
            Material yellow = Mat.glossy(c(1f, 0.8f, 0.15f)), ivory = Mat.satin(c(0.96f, 0.91f, 0.8f));
            Material black = Mat.lacquer(INK), gold = Mat.gold();
            root.add(box(gold).at(0, 0.07f, 0).sized(0.95f, 0.13f, 0.26f));
            for (int s = -1; s <= 1; s += 2) root.add(ball(Mat.satin(c(0.45f, 0.45f, 0.5f))).at(s * 0.13f, 0.17f, 0.06f).sized(0.13f, 0.08f, 0.2f));
            Material[] tailCols = { blue, red, yellow };
            for (int i = 0; i < 3; i++) root.add(ball(tailCols[i]).at((i - 1) * 0.1f, 0.3f, -0.34f).turned((i - 1) * 14, 32, 0).sized(0.12f, 0.75f, 0.05f));
            root.add(ball(red).at(0, 0.72f, 0).turned(0, -8, 0).sized(0.74f, 0.98f, 0.68f));
            root.add(ball(Mat.glossy(c(1f, 0.4f, 0.32f))).at(0, 0.62f, 0.2f).sized(0.46f, 0.6f, 0.38f));
            wingL = root.add(group().at(-0.33f, 0.92f, -0.02f));
            wingL.add(ball(blue).at(-0.04f, -0.24f, 0).turned(0, 0, 10).sized(0.17f, 0.64f, 0.42f));
            wingL.add(ball(yellow).at(-0.07f, -0.5f, -0.03f).sized(0.12f, 0.24f, 0.3f));
            wingR = root.add(group().at(0.33f, 0.92f, -0.02f));
            wingR.add(ball(blue).at(0.04f, -0.24f, 0).turned(0, 0, -10).sized(0.17f, 0.64f, 0.42f));
            wingR.add(ball(yellow).at(0.07f, -0.5f, -0.03f).sized(0.12f, 0.24f, 0.3f));
            head = root.add(group().at(0, 1.34f, 0.06f));
            head.add(ball(red).sized(0.66f, 0.62f, 0.62f));
            head.add(ball(Mat.satin(Color.WHITE)).at(0.13f, 0.03f, 0.24f).sized(0.28f, 0.3f, 0.12f));
            eye = head.add(ball(Mat.glossy(c(1f, 0.82f, 0.2f))).at(0.16f, 0.05f, 0.3f).sized(0.12f, 0.12f, 0.06f));
            eye.add(ball(Mat.lacquer(INK)).at(0.01f, 0, 0.03f).sized(0.06f, 0.06f, 0.03f));
            eye.add(ball(Mat.lit(Color.WHITE, 1.4f)).at(0.025f, 0.025f, 0.05f).sized(0.025f));
            head.add(cyl(black).at(-0.15f, 0.05f, 0.27f).turned(-22, 90, 0).sized(0.17f, 0.03f, 0.15f));
            head.add(ring(black, 0.07f).turned(0, 78, 18).sized(0.68f, 0.5f, 0.66f));
            head.add(ball(ivory).at(0, -0.03f, 0.3f).sized(0.24f, 0.22f, 0.22f));
            head.add(cone(ivory).at(0, -0.1f, 0.4f).turned(0, 118, 0).sized(0.2f, 0.34f, 0.22f));
            jaw = head.add(ball(Mat.glossy(c(0.25f, 0.22f, 0.26f))).at(0, -0.19f, 0.33f).sized(0.14f, 0.08f, 0.15f));
            Part hat = head.add(group().at(0, 0.29f, 0));
            hat.add(cyl(black).sized(0.92f, 0.06f, 0.64f));
            hat.add(cone(black).at(0, 0.17f, 0).sized(0.62f, 0.36f, 0.5f));
            hat.add(ring(gold, 0.1f).at(0, 0.02f, 0).sized(0.92f, 0.5f, 0.66f));
            hat.add(ball(Mat.satin(Color.WHITE)).at(0, 0.17f, 0.25f).sized(0.1f));
        }

        @Override
        protected void pose(float t) {
            // birds move their heads in quick little snaps
            float snap = MathUtils.floor(t * 1.6f);
            float look = MathUtils.sin(snap * 2.3f) * 14;
            head.spin(0, 1, 0, look);
            root.offset.y = MathUtils.sin(t * 2.4f) * 0.015f;
            float flap = MathUtils.sin(t * 3) * 4;
            switch (mood) {
                case TALK:
                    jaw.offset.y = -0.04f * Math.abs(MathUtils.sin(t * 12));
                    head.spin(1, 0, 0, MathUtils.sin(t * 8) * 6);
                    break;
                case CHEER:
                    flap = 40 + MathUtils.sin(t * 22) * 35;
                    root.offset.y += hop(t, 7, 0.22f);
                    jaw.offset.y = -0.05f;
                    break;
                case WOW:
                    flap = 70;
                    jaw.offset.y = -0.05f;
                    break;
                case SAD:
                    head.spin(1, 0, 0, 16);
                    flap = -6;
                    break;
                default:
                    break;
            }
            wingL.spin(0, 0, 1, -flap);
            wingR.spin(0, 0, 1, flap);
            eye.size.y *= 1 - blink(t, 4.2f, 0.8f) * 0.9f;
        }
    }
}
