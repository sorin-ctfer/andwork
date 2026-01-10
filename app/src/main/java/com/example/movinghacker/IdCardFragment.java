package com.example.movinghacker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IdCardFragment extends Fragment {
    private static final String PREFS = "id_login";
    private static final String KEY_FRONT = "front_json";
    private static final String KEY_BACK = "back_json";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MaterialButtonToggleGroup methodGroup;
    private MaterialButtonToggleGroup sideGroup;
    private TextInputLayout urlInputLayout;
    private TextInputEditText urlEdit;
    private ImageView previewImage;
    private MaterialButton actionPick;
    private MaterialButton actionCapture;
    private MaterialButton actionRecognize;
    private MaterialButton actionReset;
    private ProgressBar progress;
    private TextView resultText;

    private String selectedImageBase64;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                selectedImageBase64 = null;
                previewImage.setImageURI(uri);
                previewImage.setVisibility(View.VISIBLE);
            });

    private final ActivityResultLauncher<Void> captureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap == null) return;
                selectedImageUri = null;
                selectedImageBase64 = bitmapToBase64(bitmap);
                previewImage.setImageBitmap(bitmap);
                previewImage.setVisibility(View.VISIBLE);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_id_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        methodGroup = view.findViewById(R.id.method_group);
        sideGroup = view.findViewById(R.id.side_group);
        urlInputLayout = view.findViewById(R.id.url_input_layout);
        urlEdit = view.findViewById(R.id.url_edit);
        previewImage = view.findViewById(R.id.preview_image);
        actionPick = view.findViewById(R.id.action_pick);
        actionCapture = view.findViewById(R.id.action_capture);
        actionRecognize = view.findViewById(R.id.action_recognize);
        actionReset = view.findViewById(R.id.action_reset);
        progress = view.findViewById(R.id.progress);
        resultText = view.findViewById(R.id.result_text);

        methodGroup.check(R.id.method_camera);
        sideGroup.check(R.id.side_front);

        methodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateMethodUi(checkedId);
        });
        updateMethodUi(R.id.method_camera);

        actionPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        actionCapture.setOnClickListener(v -> captureLauncher.launch(null));
        actionRecognize.setOnClickListener(v -> onRecognizeClicked());
        actionReset.setOnClickListener(v -> {
            requireContext().getSharedPreferences(PREFS, 0).edit().clear().apply();
            selectedImageBase64 = null;
            selectedImageUri = null;
            previewImage.setVisibility(View.GONE);
            actionReset.setVisibility(View.GONE);
            resultText.setText("");
            setInputsEnabled(true);
            updateMethodUi(methodGroup.getCheckedButtonId());
        });

        refreshFromCache();
    }

    private void updateMethodUi(int methodId) {
        boolean isUrl = methodId == R.id.method_url;
        boolean isCamera = methodId == R.id.method_camera;
        boolean isUpload = methodId == R.id.method_upload;

        urlInputLayout.setVisibility(isUrl ? View.VISIBLE : View.GONE);
        actionPick.setVisibility(isUpload ? View.VISIBLE : View.GONE);
        actionCapture.setVisibility(isCamera ? View.VISIBLE : View.GONE);

        if (isUrl) {
            previewImage.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility((selectedImageUri != null || selectedImageBase64 != null) ? View.VISIBLE : View.GONE);
        }
    }

    private void refreshFromCache() {
        var prefs = requireContext().getSharedPreferences(PREFS, 0);
        boolean loggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);
        String front = prefs.getString(KEY_FRONT, null);
        String back = prefs.getString(KEY_BACK, null);

        if (!loggedIn && (front == null && back == null)) return;

        String display = buildDisplayText(front, back, loggedIn);
        resultText.setText(display);
        if (loggedIn) {
            setInputsEnabled(false);
            actionReset.setVisibility(View.VISIBLE);
        }
    }

    private void onRecognizeClicked() {
        // 检查OCR配置
        OcrConfigManager ocrConfigManager = OcrConfigManager.getInstance(requireContext());
        if (!ocrConfigManager.isConfigured()) {
            // 显示配置对话框
            OcrConfigDialog dialog = new OcrConfigDialog(requireContext());
            dialog.setOnConfigSavedListener(() -> {
                // 配置保存后重新尝试识别
                performRecognition();
            });
            dialog.show();
            return;
        }
        
        performRecognition();
    }
    
    private void performRecognition() {
        int methodId = methodGroup.getCheckedButtonId();
        int sideId = sideGroup.getCheckedButtonId();
        String cardSide = sideId == R.id.side_back ? "BACK" : "FRONT";

        String imageUrl = null;
        String imageBase64 = null;
        Uri imageUri = null;

        if (methodId == R.id.method_url) {
            imageUrl = urlEdit.getText() == null ? "" : urlEdit.getText().toString().trim();
            if (imageUrl.isEmpty()) {
                resultText.setText("请输入图片 URL。");
                return;
            }
        } else if (methodId == R.id.method_camera) {
            if (selectedImageBase64 == null || selectedImageBase64.isEmpty()) {
                resultText.setText("请先拍照。");
                return;
            }
            imageBase64 = selectedImageBase64;
        } else if (methodId == R.id.method_upload) {
            if (selectedImageUri == null) {
                resultText.setText("请先选择图片。");
                return;
            }
            imageUri = selectedImageUri;
        }

        setLoading(true);
        String finalImageUrl = imageUrl;
        String finalImageBase64 = imageBase64;
        Uri finalImageUri = imageUri;
        executor.execute(() -> {
            try {
                String base64 = finalImageBase64;
                if (base64 == null && finalImageUri != null) {
                    base64 = uriToBase64(finalImageUri);
                    if (base64 == null || base64.isEmpty()) {
                        throw new IllegalStateException("图片读取失败。");
                    }
                }
                String responseStr = IdCardOcrClient.idCardOcr(requireContext(), base64, finalImageUrl, cardSide);
                JSONObject root = new JSONObject(responseStr);
                JSONObject resp = root.optJSONObject("Response");
                if (resp != null && resp.has("Error")) {
                    JSONObject err = resp.optJSONObject("Error");
                    String msg = err == null ? "识别失败" : err.optString("Message", "识别失败");
                    throw new IllegalStateException(msg);
                }

                var prefs = requireContext().getSharedPreferences(PREFS, 0);
                var editor = prefs.edit();
                if ("FRONT".equals(cardSide)) {
                    editor.putString(KEY_FRONT, responseStr);
                } else {
                    editor.putString(KEY_BACK, responseStr);
                }
                String front = "FRONT".equals(cardSide) ? responseStr : prefs.getString(KEY_FRONT, null);
                String back = "BACK".equals(cardSide) ? responseStr : prefs.getString(KEY_BACK, null);
                boolean loggedIn = front != null && back != null;
                editor.putBoolean(KEY_LOGGED_IN, loggedIn);
                editor.apply();

                String display = buildDisplayText(front, back, loggedIn);
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    resultText.setText(display);
                    if (loggedIn) {
                        setInputsEnabled(false);
                        actionReset.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    resultText.setText(e.getMessage() == null ? "识别失败" : e.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        actionRecognize.setEnabled(!loading);
        actionPick.setEnabled(!loading);
        actionCapture.setEnabled(!loading);
        methodGroup.setEnabled(!loading);
        sideGroup.setEnabled(!loading);
    }

    private void setInputsEnabled(boolean enabled) {
        methodGroup.setVisibility(enabled ? View.VISIBLE : View.GONE);
        sideGroup.setVisibility(enabled ? View.VISIBLE : View.GONE);
        urlInputLayout.setVisibility(enabled && methodGroup.getCheckedButtonId() == R.id.method_url ? View.VISIBLE : View.GONE);
        actionPick.setVisibility(enabled && methodGroup.getCheckedButtonId() == R.id.method_upload ? View.VISIBLE : View.GONE);
        actionCapture.setVisibility(enabled && methodGroup.getCheckedButtonId() == R.id.method_camera ? View.VISIBLE : View.GONE);
        actionRecognize.setVisibility(enabled ? View.VISIBLE : View.GONE);
        progress.setVisibility(View.GONE);
    }

    private String buildDisplayText(String frontJson, String backJson, boolean loggedIn) {
        StringBuilder sb = new StringBuilder();
        if (loggedIn) {
            sb.append("已登录（本地缓存）\n\n");
        } else {
            sb.append("已缓存部分信息（继续识别另一面即可完成登录）\n\n");
        }

        if (frontJson != null) {
            sb.append("人像面\n");
            try {
                JSONObject resp = new JSONObject(frontJson).optJSONObject("Response");
                sb.append("姓名：").append(opt(resp, "Name")).append("\n");
                sb.append("性别：").append(opt(resp, "Sex")).append("\n");
                sb.append("民族：").append(opt(resp, "Nation")).append("\n");
                sb.append("出生：").append(opt(resp, "Birth")).append("\n");
                sb.append("住址：").append(opt(resp, "Address")).append("\n");
                sb.append("身份证号：").append(opt(resp, "IdNum")).append("\n\n");
            } catch (Exception ignored) {
                sb.append("解析失败\n\n");
            }
        } else {
            sb.append("人像面：未识别\n\n");
        }

        if (backJson != null) {
            sb.append("国徽面\n");
            try {
                JSONObject resp = new JSONObject(backJson).optJSONObject("Response");
                sb.append("签发机关：").append(opt(resp, "Authority")).append("\n");
                sb.append("有效期限：").append(opt(resp, "ValidDate")).append("\n");
            } catch (Exception ignored) {
                sb.append("解析失败\n");
            }
        } else {
            sb.append("国徽面：未识别\n");
        }
        return sb.toString();
    }

    private static String opt(JSONObject obj, String key) {
        if (obj == null) return "";
        return obj.optString(key, "");
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap;
            try (InputStream input = is) {
                bitmap = decodeSampledBitmapFromStream(input, 1600, 1600);
            }
            if (bitmap == null) return null;
            return bitmapToBase64(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, int reqHeight) throws Exception {
        byte[] data = readAllBytes(is);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
