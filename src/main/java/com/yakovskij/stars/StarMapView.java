package com.yakovskij.stars;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

@SuppressLint("ViewConstructor")
public class StarMapView extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isRunning;
    private SurfaceHolder holder;
    private Paint paint;
    List<DataObjects.Star> stars = null;
    List<DataObjects.ConstellationLinesMapping> constellationLines = null;
    private float cameraAzimuth = 0;
    private float cameraAltitude = 0;
    public float etalonWidthFOVgradus = 30.0f;

    public int canvasWidth, canvasHeight;

    public float coefXYCanvas;
    private float scaleFactor = 1.0f;
    public float widthFOVgradus, heightFOVgradus;
    public boolean draw_constellations = true;
    public boolean draw_labels = true;
    public boolean draw_horizon = true;
    public boolean draw_grid = true;
    public boolean draw_stars = true;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private float touchStartX, touchStartY;


    public StarMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        coefXYCanvas = 1.5f;


        gestureDetector = new GestureDetector(getContext(), new StarMapView.GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new StarMapView.ScaleListener());

    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            handleStarClick(e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            cameraAzimuth -= distanceX / 10.0f * scaleFactor;
            cameraAltitude -= distanceY / 10.0f * scaleFactor;

            if (cameraAzimuth >= 360)
                cameraAzimuth -= 360;
            else if (cameraAzimuth < 0)
                cameraAzimuth += 360;

            cameraAltitude = Math.max(-90, Math.min(90, cameraAltitude));

            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor /= detector.getScaleFactor();
            scaleFactor = Math.max(0.05f, Math.min(scaleFactor, 2.0f)); // Ограничиваем масштабирование

            widthFOVgradus = etalonWidthFOVgradus * scaleFactor;
            heightFOVgradus = widthFOVgradus * canvasHeight / canvasWidth;

            return true;
        }
    }

    private void handleStarClick(float touchX, float touchY) {
        if (stars == null || stars.isEmpty()) return;

        if (draw_stars) {
            // 2. Ограничиваем поиск по азимуту и высоте (примерный допуск 5°)
            for (int i = 0; i < stars.size(); i++) {
                DataObjects.Star star = stars.get(i);

                float azDiff = Math.abs(star.az - cameraAzimuth);
                if (azDiff > 180) azDiff = 360 - azDiff; // Корректно учитываем переход через 0°

                float altDiff = Math.abs(star.alt - cameraAltitude);

// Если звезда сильно далеко по азимуту/высоте - пропускаем
                if (azDiff > widthFOVgradus || altDiff > heightFOVgradus) continue;
                float[] screenCoords = projectToScreen(star.az, star.alt);

                float distance = (float) Math.sqrt(Math.pow(touchX - screenCoords[0], 2) +
                        Math.pow(touchY - screenCoords[1], 2));

                float starSize = (1 / scaleFactor) * 5 * (1 - star.mag / 10); // Радиус звезды
                starSize *= 3;

                if (distance < starSize * 2) {
                    showStarInfo(star);
                    return;
                }
            }
        }

        if (draw_constellations) {
            // 2. Проверяем нажатие на линию созвездия
            float threshold = 20; // Максимально допустимое расстояние до линии
            for (DataObjects.ConstellationLinesMapping constellation : constellationLines) {
                for (DataObjects.ConstellationLine line : constellation.lines) {
                    float[] startCoords = projectToScreen((float) line.az0, (float) line.alt0);
                    float[] endCoords = projectToScreen((float) line.az1, (float) line.alt1);

                    if (distanceToSegment(touchX, touchY, startCoords[0], startCoords[1], endCoords[0], endCoords[1]) < threshold) {
                        showConstellationInfo(constellation.constellation);
                        return;
                    }
                }
            }
        }
    }
    private void showStarInfo(DataObjects.Star star) {
        StarDialogFragment.newInstance(star.hip)
                .show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "star_info");

    }
    private void showConstellationInfo(DataObjects.Constellation constellation) {
        ConstellationDialogFragment.newInstance(constellation.id, constellation.name)
                .show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "constellation_dialog");
    }


    // Функция вычисления расстояния от точки (px, py) до отрезка (x1, y1) - (x2, y2)
    private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return (float) Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        }

        float t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        return (float) Math.sqrt(Math.pow(px - closestX, 2) + Math.pow(py - closestY, 2));
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void run() {

        while (isRunning) {
            if (!holder.getSurface().isValid()) continue;
            Canvas canvas = holder.lockCanvas();

            canvasWidth = canvas.getWidth();
            canvasHeight = canvas.getHeight();
            widthFOVgradus = etalonWidthFOVgradus * scaleFactor;
            heightFOVgradus = widthFOVgradus * canvasHeight / canvasWidth;


            drawBackground(canvas);
            if(draw_grid)
                drawPolarGrid(canvas);
            if (constellationLines != null && draw_constellations)
                drawConstellations(canvas);
            if(stars != null && draw_stars)
                drawSky(canvas);
            if (draw_horizon)
                drawHorizont(canvas);

            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            canvas.drawText(String.format("Azim: %.1f", cameraAzimuth), 10, 70, paint);
            canvas.drawText(String.format("Atitude: %.1f", cameraAltitude), 10, 140, paint);

            paint.setTextSize(50);

            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.rgb(5, 5, 42)); // Темно-голубое небо
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSky(Canvas canvas) {

        paint.setStyle(Paint.Style.FILL);
        for (DataObjects.Star star : stars) {
            // Преобразование сферических координат в декартовы

            float[] starCoords = projectToScreen(star.az, star.alt);

            // Установка цвета звезды
            paint.setColor(Color.rgb((int) (star.r * 255), (int) (star.g * 255), (int) (star.b * 255)));

            // Рисование звезды
            float starSize = (1 / scaleFactor) * 5 * (1 - star.mag / 10); // Примерная зависимость размера от яркости
            canvas.drawCircle(starCoords[0], starCoords[1], starSize, paint);
        }
    }
    private void drawConstellations(Canvas canvas){
        // Настраиваем стиль для линий созвездий
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        // Рисуем линии созвездий
        for (DataObjects.ConstellationLinesMapping constellation : constellationLines) {
            float[] first_star = null;
            for (DataObjects.ConstellationLine line : constellation.lines) {
                float[] startCoords = projectToScreen((float) line.az0, (float) line.alt0);
                float[] endCoords = projectToScreen((float) line.az1, (float) line.alt1);
                if (first_star == null)
                    first_star = endCoords;

                canvas.drawLine(startCoords[0], startCoords[1], endCoords[0], endCoords[1], paint);

            }

            if (first_star != null && draw_labels) {
                // Подписываем созвездие (например, название берем из mapping.constellation.name)
                // Меняем стиль для текста
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(40);
                // Если minX/minY слишком близко к краю, добавляем небольшой отступ
                float textX = first_star[0] - 10;
                float textY = first_star[1] - 40;
                canvas.drawText(constellation.constellation.name, textX, textY, paint);
                // Восстанавливаем стиль для линий
                paint.setStyle(Paint.Style.STROKE);
            }
        }
    }

    private void drawPolarGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Рисуем полярную сетку
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        // Линии по азимуту
        for (int az = 0; az < 360; az += 10) {
            Path path = new Path();
            boolean firstPoint = true;
            for (int alt = -90; alt <= 90; alt += 1) {
                float[] screenCoords = projectToScreen(az, alt);
                if (firstPoint) {
                    path.moveTo(screenCoords[0], screenCoords[1]);
                    firstPoint = false;
                } else {
                    path.lineTo(screenCoords[0], screenCoords[1]);
                }
            }
            canvas.drawPath(path, paint);
        }

        // Линии по высоте
        for (int alt = -80; alt <= 80; alt += 10) {
            Path path = new Path();
            boolean firstPoint = true;
            for (int az = 0; az <= 360; az += 1) {
                float[] screenCoords = projectToScreen(az, alt);
                if (firstPoint) {
                    path.moveTo(screenCoords[0], screenCoords[1]);
                    firstPoint = false;
                } else {
                    path.lineTo(screenCoords[0], screenCoords[1]);
                }
            }
            canvas.drawPath(path, paint);
        }

    }
    private float[] projectToScreen(float az, float alt) {
        // Преобразование сферических координат в декартовы
        float x = (float) (Math.cos(Math.toRadians(alt)) * Math.sin(Math.toRadians(az)));
        float y = (float) (Math.cos(Math.toRadians(alt)) * Math.cos(Math.toRadians(az)));
        float z = (float) Math.sin(Math.toRadians(alt));

        float azRad = (float) Math.toRadians(cameraAzimuth - 180);
        float altRad = (float) Math.toRadians(cameraAltitude - 90);

        // Вращение вокруг оси Z (азимут)
        float rotatedX = x * (float) Math.cos(azRad) - y * (float) Math.sin(azRad);
        float rotatedY = x * (float) Math.sin(azRad) + y * (float) Math.cos(azRad);
        float rotatedZ = z;

        // Вращение вокруг оси X (альтитуда)
        float finalX = rotatedX;
        float finalY = rotatedY * (float) Math.cos(altRad) - rotatedZ * (float) Math.sin(altRad);
        float finalZ = rotatedY * (float) Math.sin(altRad) + rotatedZ * (float) Math.cos(altRad);

        // Проекция на 2D-плоскость
        float projectedX = finalX / (finalZ + 1); // Простая перспектива
        float projectedY = finalY / (finalZ + 1);
// Масштабирование в зависимости от FOV
        float scaleX = canvasWidth / (2 * (float) Math.tan(Math.toRadians(widthFOVgradus / 2)));
        float scaleY = canvasHeight / (2 * (float) Math.tan(Math.toRadians(heightFOVgradus / 2)));

// Перевод в пиксели на экране
        float screenX = (float) canvasWidth / 2 + projectedX * scaleX;
        float screenY = (float) canvasHeight / 2 - projectedY * scaleY;
        return new float[]{screenX, screenY};
    }
    private void drawHorizont(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        paint.setColor(Color.rgb(0, 99, 46));
        paint.setStyle(Paint.Style.FILL);

        // Получаем центральную точку горизонта (на уровне 0 градусов высоты)
        float[] horizonCenter = projectToScreen(cameraAzimuth, -20);

        // Если центральная точка не определена, задаём горизонт по умолчанию
        if (horizonCenter == null) {
            return;
        }

        // Определяем горизонтальное масштабирование
        float scaleX = width / (2 * (float) Math.tan(Math.toRadians(widthFOVgradus / 2)));

        // Вычисляем смещение в пикселях для половины поля зрения
        float deltaX = scaleX * (float) Math.tan(Math.toRadians(widthFOVgradus / 2));

        // Используем центральную точку для определения y-координаты горизонта
        float horizonY = horizonCenter[1];

        // Вычисляем точки по горизонтали с поправкой на масштаб:
        // Левая точка: x = центр минус deltaX (но не меньше 0)
        float leftX = Math.max(0, horizonCenter[0] - deltaX);
        // Правая точка: x = центр плюс deltaX (но не больше ширины экрана)
        float rightX = Math.min(width, horizonCenter[0] + deltaX);

        // Строим путь горизонта. Для плавности можно использовать кривую через центр.
        Path path = new Path();
        path.moveTo(0, horizonY);          // Начинаем с левого края экрана
        path.quadTo(horizonCenter[0], horizonY - 50, width, horizonY);  // Кривая через центр (20 пикселей вверх)
        path.lineTo(width, height);        // Спускаемся до нижнего края
        path.lineTo(0, height);            // Возвращаемся к левому нижнему углу
        path.close();

        canvas.drawPath(path, paint);
    }



    public void start() {

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
