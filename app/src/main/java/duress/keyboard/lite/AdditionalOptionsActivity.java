package duress.keyboard.lite;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.util.*;
import java.util.*;

public class AdditionalOptionsActivity extends Activity {

	private void showWipeLimitDialog() {
    final android.widget.EditText input = new android.widget.EditText(this);
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    android.widget.FrameLayout container = new android.widget.FrameLayout(this);
    android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(-1, -2);
    int margin = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
    params.leftMargin = margin; params.rightMargin = margin;
    input.setLayoutParams(params);
    container.addView(input);
	String dialogLangText="Hello. This is Auto-Wipe Settings. Here you can set limit of failed password attempts to wipe phone data. This wipe can be initialized by system (even in safe mode) and can be launched by entering any type of password longer (>=) than 4 characters. System may display warning about unsuccessful attempts. Set any number. If you want wipe immediatelly after 1 incorrect attempt, set 1. if you set 0 - this is no limit.";
       
	
	if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) dialogLangText="Привет. Это Настройки Авто-Сброса. Сдесь вы можете установить лимит на количество неудачных попыток ввода пароля для очистки данных телефона. Эта очистка может быть инициирована системой (даже в безопасном режиме) и быть запущена вводом любого типа пароля длиннее (>=) 4 символов. Система может показывать предупрежление о неудачных попытках. Установите любое число. Если хотите, чтобы очистка происходила немедленно после 1 неверной попытки, установите 1. Если установите 0 - это нет лимита.";

    final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setMessage(dialogLangText).setView(container).setCancelable(false).setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
        @Override
        public void onClick(android.content.DialogInterface dialog, int which) {
            String val = input.getText().toString();
            if (val.isEmpty()) {showWipeLimitDialog(); return;}
            try {
                int limit = Integer.parseInt(val);
                android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(android.content.Context.DEVICE_POLICY_SERVICE);
                android.content.ComponentName adminComponent = new android.content.ComponentName(AdditionalOptionsActivity.this, MyDeviceAdminReceiver.class);
				dpm.setMaximumFailedPasswordsForWipe(adminComponent, limit);                
                int factLimit = dpm.getMaximumFailedPasswordsForWipe(adminComponent);				
                android.widget.Toast.makeText(AdditionalOptionsActivity.this, "Password failed attempts for wipe: " + factLimit + ".", android.widget.Toast.LENGTH_LONG).show();
				showWipeLimitDialog();
				try {					
					finish();					
				} catch (Throwable tirex) {}
				return;
			} catch (Throwable t) {
                android.widget.TextView errorView = new android.widget.TextView(AdditionalOptionsActivity.this);
                errorView.setText(t.getMessage()); errorView.setTextIsSelectable(true); errorView.setPadding(60, 40, 60, 0);
                new android.app.AlertDialog.Builder(AdditionalOptionsActivity.this).setTitle("Err:").setView(errorView).setPositiveButton("OK", null).show();
            }
        }
    }).create();
    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    dialog.getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    
	android.view.Window window = dialog.getWindow();
	if (window != null) {
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    android.view.WindowManager.LayoutParams lp = window.getAttributes();
    lp.gravity = android.view.Gravity.CENTER;
    lp.y = 0; 
    lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT; 
    window.setAttributes(lp);
	}
	dialog.show();
	input.requestFocus();
    input.requestFocus();
	}

    @Override
    protected void onResume() {
        super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
		getWindow().getDecorView().setKeepScreenOn(true);
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
		}
	
	
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        showWipeLimitDialog();
    }
}
