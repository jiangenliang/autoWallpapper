package com.example.autowallpaper; // 替换为您的包名

import androidx.appcompat.app.AppCompatActivity; // 关键！如果没有会飘红
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private Button btnSelectFolder, btnStart, btnAddShortcut;
    private EditText etInterval;
    private String wallpaperFolderPath;
    private int intervalMinutes = 10;
    private Handler handler;
    private Runnable wallpaperRunnable;
    private List<String> wallpaperList;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 关联布局文件

        // 初始化组件
        btnSelectFolder = findViewById(R.id.btn_select_folder);
        btnStart = findViewById(R.id.btn_start);
        btnAddShortcut = findViewById(R.id.btn_add_shortcut);
        etInterval = findViewById(R.id.et_interval);

        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        wallpaperList = new ArrayList<>();

        // 选择文件夹
        btnSelectFolder.setOnClickListener(v -> selectFolder());

        // 开始自动更换
        btnStart.setOnClickListener(v -> startAutoChange());

        // 添加快捷方式
        btnAddShortcut.setOnClickListener(v -> addShortcut());
    }

    // 选择壁纸文件夹
    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 处理Android10+文件路径
                String docId = DocumentsContract.getTreeDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    wallpaperFolderPath = getExternalFilesDir(null) + "/" + split[1];
                }
                loadWallpapers();
                Toast.makeText(this, "已选择文件夹：" + wallpaperFolderPath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 加载壁纸列表
    private void loadWallpapers() {
        wallpaperList.clear();
        File folder = new File(wallpaperFolderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String ext = getFileExtension(file.getName());
                    if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("png")) {
                        wallpaperList.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // 开始自动更换
    private void startAutoChange() {
        String intervalStr = etInterval.getText().toString();
        if (!intervalStr.isEmpty()) intervalMinutes = Integer.parseInt(intervalStr);

        // 停止之前的任务
        if (wallpaperRunnable != null) handler.removeCallbacks(wallpaperRunnable);

        // 定时任务
        wallpaperRunnable = () -> {
            if (!wallpaperList.isEmpty()) {
                String path = wallpaperList.get(random.nextInt(wallpaperList.size()));
                setWallpaper(path);
            }
            handler.postDelayed(wallpaperRunnable, intervalMinutes * 60 * 1000);
        };

        handler.post(wallpaperRunnable);
        Toast.makeText(this, "自动更换已启动，每" + intervalMinutes + "分钟更换", Toast.LENGTH_SHORT).show();
    }

    // 设置壁纸
    private void setWallpaper(String path) {
        try {
            android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance(this);
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            wallpaperManager.setBitmap(bitmap);
            Toast.makeText(this, "壁纸已更换", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "设置壁纸失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加桌面快捷方式
    private void addShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android8.0+使用ShortcutManager
            android.content.pm.ShortcutManager shortcutManager = getSystemService(android.content.pm.ShortcutManager.class);
            if (shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutIntent = new Intent(this, MainActivity.class);
                shortcutIntent.setAction(Intent.ACTION_MAIN);

                android.content.pm.ShortcutInfo shortcutInfo = new android.content.pm.ShortcutInfo.Builder(this, "wallpaper_shortcut")
                        .setIntent(shortcutIntent)
                        .setShortLabel("一键换壁纸")
                        .setLongLabel("一键更换壁纸")
                        .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                        .build();

                shortcutManager.requestPinShortcut(shortcutInfo, null);
            }
        } else {
            // 旧版本方法
            Intent shortcutIntent = new Intent(this, MainActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);

            Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "一键换壁纸");
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));

            sendBroadcast(intent);
        }
        Toast.makeText(this, "快捷方式已添加", Toast.LENGTH_SHORT).show();
    }
}

