package duress.keyboard.lite;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.text.method.*;
import android.text.style.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends Activity {


	private android.app.AlertDialog accessibilityDialog;
	private static boolean main=true;
	boolean accessibilityEnabled = false;
    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";
	private BroadcastReceiver screenOffReceiver;
	private SharedPreferences prefsNetwork;
	
	private Switch noNetworkWipeSwitch;
	private static final String KEY_LAYOUT_RU = "layout_ru";
    private static final String KEY_LAYOUT_EN = "layout_en";
    private static final String KEY_LAYOUT_SYM = "layout_sym";
    private static final String KEY_LAYOUT_EMOJI = "layout_emoji";
    private static final String KEY_LAYOUT_ES = "layout_es";
	private static boolean RESULT = false;
	private EditText commandInput; 
    private static final String KEY_LANG_RU = "lang_ru";
    private static final String KEY_LANG_EN = "lang_en";
    private static final String KEY_LANG_SYM = "lang_sym";
    private static final String KEY_LANG_EMOJI = "lang_emoji";
    private static final String KEY_LANG_ES = "lang_es";
	private static int e= 0;


	

	private LinearLayout layout;

	private int dpToPx(int dp) {    
		float density = getResources().getDisplayMetrics().density;    
		return (int) (dp * density + 0.5f);    
	}  

	private String getAllowedCharacters(Context context) {
		Set<String> charSet = new HashSet<>();
		Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
		SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		String[] keys = {KEY_LAYOUT_RU, KEY_LAYOUT_EN, KEY_LAYOUT_ES, KEY_LAYOUT_SYM, KEY_LAYOUT_EMOJI};

		for (String key : keys) {
			String jsonString = prefs.getString(key, "[]");
			try {
				JSONArray outer = new JSONArray(jsonString);
				for (int i = 0; i < outer.length(); i++) {
					JSONArray inner = outer.getJSONArray(i);
					for (int j = 0; j < inner.length(); j++) {
						String symbol = inner.getString(j);

						if (symbol.length() == 1 || symbol.length() > 1 && Character.isSurrogatePair(symbol.charAt(0), symbol.charAt(1))) {
							charSet.add(symbol);
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}


		charSet.remove(" "); 


		charSet.remove("⇪"); // Shift
		charSet.remove("⌫"); // Backspace
		charSet.remove("!#?"); // Sym switch
		charSet.remove("abc"); // Alpha switch
		charSet.remove("🌐"); // Lang switch
		charSet.remove("⏎"); // Enter/Wipe trigger


		StringBuilder sb = new StringBuilder();
		for (String s : charSet) {
			sb.append(s);
		}
		return sb.toString();
	}




	private String generateSalt() {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt);
	}



	private String hashKeyWithSalt(String salt, String cmd) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hashBytes = digest.digest((salt + cmd).getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hashBytes);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		RESULT=false;
		if (screenOffReceiver != null) {
			unregisterReceiver(screenOffReceiver);
			screenOffReceiver = null;
		}
	}


    @Override
    protected void onResume() {
        super.onResume();

		if (RESULT==true){
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);

			ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
			DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

			if (!dpm.isAdminActive(adminComponent)) {
				Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
				String explanation;
				if ("ru".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
					explanation = "Дайте разрешение Администратора. Необходимо для работы функции стирания данных. Стирает данные только когда вы зададите свой код в этом приложении, и введете на экране блокировки используя клавитуру этого приложения и нажмёте стрелку Enter (⏎).";
				} else {
					explanation = "Grant Administrator permission. This is required for the data wipe feature to work. Data will only be wiped when you set your code in this app, and enter on the lock screen using the app's keyboard and press the Enter arrow (⏎).";
				}
				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation);
				startActivity(intent);
			}


		}}

	private void showLanguageSelectionDialog() {
		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		final boolean isRussianDevice = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());


		final String[] languages = new String[] {
			"Русский (Russian)",
			"English (English)",
			"Español (Spanish)",
			isRussianDevice ? "Символы (!#?)": "Symbols (!#?)",
			isRussianDevice ? "Эмодзи (😡🤡👍)" : "Emoji (😡🤡👍)"
		};

		final String[] keys = {KEY_LANG_RU, KEY_LANG_EN, KEY_LANG_ES, KEY_LANG_SYM, KEY_LANG_EMOJI};
		final boolean[] checkedItems = new boolean[languages.length];


		for (int i = 0; i < keys.length; i++) {
			checkedItems[i] = prefs.getBoolean(keys[i], false);
		}



		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(isRussianDevice ? "Выберите языки сервиса клавиатуры" : "Select keyboard service languages")
			.setMultiChoiceItems(languages, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					checkedItems[which] = isChecked;
				}
			})
			.setPositiveButton(isRussianDevice ? "Сохранить" : "Save", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences.Editor ed = prefs.edit();
					for (int i = 0; i < keys.length; i++) {
						ed.putBoolean(keys[i], checkedItems[i]);
					}
					ed.apply();



					Toast.makeText(MainActivity.this,
								   isRussianDevice ? "Языки сервиса клавиатуры сохранены" : "Keyboard service languages saved",
								   Toast.LENGTH_SHORT).show();



				}
			})
			.setNegativeButton(isRussianDevice ? "Отмена" : "Cancel", null)
			.show();
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
					RESULT = false;
                    finish();
                }
            }
        };
        registerReceiver(screenOffReceiver, filter);

        String sysLang = Locale.getDefault().getLanguage();
        final boolean isRussianDevice = "ru".equalsIgnoreCase(sysLang);


        initializeDefaultLayoutsIfNeeded(isRussianDevice);

        initializeDefaultLanguageFlagsIfNeeded(isRussianDevice);


		commandInput = new EditText(this);
		commandInput.setHint(isRussianDevice ? "Задайте команду для сброса данных" : "Set wipe data command");

		final String allowedChars = getAllowedCharacters(this);


		InputFilter filter1 = new InputFilter.LengthFilter(50);


		InputFilter filterChars = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end, 
									   Spanned dest, int dstart, int dend) {


				for (int i = start; i < end; i++) {
					if (allowedChars.indexOf(source.charAt(i)) == -1) {
						return ""; // Отклонить символ
					}
				}
				return null; // Принять ввод
			}
		};


		commandInput.setFilters(new InputFilter[] { filter1, filterChars });




		final Button saveButton = new Button(this);
		saveButton.setText(isRussianDevice ? "Сохранить команду" : "Save command");

		saveButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					String cmd = commandInput.getText().toString().trim();
					if (!cmd.isEmpty()) {
						try {

							String salt = generateSalt();
							String commandHash = hashKeyWithSalt(salt, cmd);


							Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
							SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

							prefs.edit()
								.putString(KEY_CUSTOM_COMMAND, commandHash)
								.putString("command_salt", salt)
								.apply();


							String inputHash="";

							try
							{
								MessageDigest digest = MessageDigest.getInstance("SHA-256");
								byte[] hashBytes = digest.digest((salt + cmd).getBytes(StandardCharsets.UTF_8));
								inputHash = Base64.getEncoder().encodeToString(hashBytes);

							}
							catch (Exception e)
							{}  


							if (commandHash.equals(inputHash)) {

								Toast.makeText(MainActivity.this, 
											   (isRussianDevice ? "Команда сохранена: " : "Command saved: ") + cmd, 
											   Toast.LENGTH_SHORT).show();
							} 

							if (!commandHash.equals(inputHash)) {

								Toast.makeText(MainActivity.this, 
											   (isRussianDevice ? "Ошибка! Хеши не совпадают!" : "Error! Hashes Not Match!"),
											   Toast.LENGTH_SHORT).show();		   				   
							}




							commandInput.setText("");
							commandInput.clearFocus();
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow(commandInput.getWindowToken(), 0);

						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							Toast.makeText(MainActivity.this, "Ошибка хеширования", Toast.LENGTH_SHORT).show();
						}
					}
				}
			});


		
		final Button readInstructionsButton = new Button(this);
		readInstructionsButton.setText(isRussianDevice ? "Прочитать подробную инструкцию" : "Read detailed instructions");

		readInstructionsButton.setOnClickListener(new View.OnClickListener() {


				private static final String in_ru="Привет. Это приложение DuressKeyboardLite. Это клон приложения DuressKeyboard, но с урезанным функционалом (только Duress Password) и более высоким target sdk. Duress Password это функция, которая позволяет стиреть данные при вводе специального пароля на экране блокировки. Например если вы хотите сбросить данные когда вас заставляют ввести пароль, и тогда вы вводите Duress Passoword вместо обычного. По сути этого же можно добиться просто использовав DevicePolicyManager.setMaximumFailedPasswordsForWipe(AdminReveiver, 1) из любого приложения администратора устройства, и тогда любой неверный пароль станет Duress паролем. Так как неверный пароль можно ввести случайно, данное приложение делает умнее. Оно устанавливает DevicePolicyManager.setMaximumFailedPasswordsForWipe(AdminReveiver, 1) именно при вводе Duress пароля на экране блокировки, а затем просто отправляет его в систему, и если он не соответствует паролю разблокировки, то данные сотрутся. Можно было бы сделать проще: делать DevicePolicyManager.wipeData(0), тогда можно было реагировать и на другие тригеры на сброс и сделать функционал более широким. Именно это реализованно в обычном приложении DuressKeyboard. В этом же приложении это реализованно быть не может из-за более высокого target sdk>=34, который не позволяет выполнять dpm.WipeData для user 0 будучи обычным Device Admin. Внимание! Это приложение приложение может работать только если у вас установлен текстовый пароль (а не пин-код) и если в вашей системе разрешно работать сторонним клавиатурам на экране блокировки. Вы легко увидите это. Если не разрешено, то вы можете установить обычный DuressKeyboard и заглянуть в дополнительнын параметры. Там есть функция которая работает через специальные возможности для таких случаев. Кстати, если вы таки хотите установить его, но не можете из-за того что система считает его устаревшим, вы можете использовать adb shell pm install --bypass-low-target-sdk-block /storage/emulated/0/Download/file_name_in_phone.apk с помощью компьютера и отладки по USB (SDK Platform-Tools) или отладки по WiFi (через программы Shizuku: https://github.com/RikkaApps/Shizuku/releases/latest + aShell: https://f-droid.org/packages/in.sunilpaulmathew.ashell). Кстати, желательно отключить другие клавиатуры (после того как вы включили эту) для того чтобы вас никто не мог заставить переключиться на них на экране блокировки. Сделать это можно через adb shell pm disable-user --user 0 package.name (а если вы не можете найти их в настройках или пакет не отображается, то используйте Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager)).";
				private static final String in_en="​Hello. This is the DuressKeyboardLite app. This is a clone of the DuressKeyboard app, but with stripped-down functionality (only Duress Password) and a higher target SDK. Duress Password is a feature that allows wiping data when a special password is entered on the lock screen. For example, if you want to reset data when you are forced to enter a password, and then you enter the Duress Password instead of the regular one. Essentially, the same can be achieved simply by using DevicePolicyManager.setMaximumFailedPasswordsForWipe(AdminReveiver, 1) from any device administrator app, and then any incorrect password will become a Duress password. Since an incorrect password can be entered accidentally, this app does it smarter. It sets DevicePolicyManager.setMaximumFailedPasswordsForWipe(AdminReveiver, 1) precisely upon entering the Duress password on the lock screen, and then simply sends it to the system, and if it does not match the unlock password, the data will be wiped. It could have been done simpler: by doing DevicePolicyManager.wipeData(0), then it would be possible to react to other reset triggers and make the functionality broader. This is exactly what is implemented in the regular DuressKeyboard app. In this app, however, it cannot be implemented due to the higher target SDK >= 34, which does not allow executing dpm.WipeData for user 0 while being a regular Device Admin. Attention! This app can only work if you have a text password set (not a PIN code) and if your system allows third-party keyboards to work on the lock screen. You will easily see this. If it is not allowed, you can install the regular DuressKeyboard and look into the additional parameters. There is a feature there that works through Accessibility services for such cases. By the way, if you still want to install it but cannot because the system considers it outdated, you can use adb shell pm install --bypass-low-target-sdk-block /storage/emulated/0/Download/file_name_in_phone.apk using a computer and USB debugging (SDK Platform-Tools) or via WiFi debugging (using Shizuku programs: https://github.com/RikkaApps/Shizuku/releases/latest + aShell: https://f-droid.org/packages/in.sunilpaulmathew.ashell). Also, it is advisable to disable other keyboards (after you have enabled this one) so that no one can force you to switch to them on the lock screen. You can do this via adb shell pm disable-user --user 0 package.name (and if you cannot find them in the settings or the package is not displayed, use Package Manager (https://f-droid.org/en/packages/com.smartpack.packagemanager)).";

			    @Override
				public void onClick(View v) {

					String instructions;

					if (isRussianDevice) {
						instructions = in_ru;
					} else {
						instructions = in_en;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

					ScrollView scroll = new ScrollView(MainActivity.this);
					int padding = (int) (16 * getResources().getDisplayMetrics().density);

					TextView tv = new TextView(MainActivity.this);
					tv.setText(instructions);
					tv.setTextColor(Color.BLACK);
					tv.setTextSize(16);
					tv.setPadding(padding, padding, padding, padding);
					tv.setTextIsSelectable(true); 


					String text = instructions;

					SpannableString ss = new SpannableString(text);


					Pattern pattern = Pattern.compile("(https?://[A-Za-z0-9/.:\\-_%?=&]+)");
					Matcher matcher = pattern.matcher(text);

					while (matcher.find()) {
						final String url = matcher.group();

						ss.setSpan(
							new ClickableSpan() {
								@Override
								public void onClick(View widget) {
									Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
									widget.getContext().startActivity(intent);
								}

								@Override
								public void updateDrawState(TextPaint ds) {
									super.updateDrawState(ds);
									ds.setColor(Color.BLUE);
									ds.setUnderlineText(true);
								}
							},
							matcher.start(),
							matcher.end(),
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
						);
					}

					tv.setText(ss);
					tv.setMovementMethod(LinkMovementMethod.getInstance());
					tv.setLinksClickable(true);
					tv.setTextColor(Color.BLACK);
					tv.setTextIsSelectable(true);
					scroll.addView(tv);

					builder.setTitle(isRussianDevice ? "Инструкция" : "Instructions");
					builder.setView(scroll);
					builder.setPositiveButton("OK", null);
					builder.show();
				}
			});


		final Button keyboardSettingsButton = new Button(this);
		keyboardSettingsButton.setText(isRussianDevice ? "Открыть настройки клавиатур чтобы включить нашу." : "Open keyboard settings to enable our.");
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
				}
			});


		final Button chooseKeyboardButton = new Button(this);
		chooseKeyboardButton.setText(isRussianDevice ? "Выбрать нашу клавиатуру если включена" : "Choose our keyboard if enabled");
		chooseKeyboardButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showInputMethodPicker();
					} else {
						Toast.makeText(MainActivity.this, isRussianDevice ? "Не удалось открыть выбор клавиатуры" : "Failed to open keyboard picker", Toast.LENGTH_SHORT).show();
					}
				}
			});

		

		



        final Button selectLanguagesButton = new Button(this);
		selectLanguagesButton.setText(isRussianDevice ? "Выбрать языки сервиса клавиатуры" :
									  "Select keyboard service languages");
		selectLanguagesButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLanguageSelectionDialog();
				}
			});



        

		layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(commandInput);
        layout.addView(saveButton);
		layout.addView(keyboardSettingsButton);
		layout.addView(chooseKeyboardButton);
        layout.addView(selectLanguagesButton);
		layout.addView(readInstructionsButton);
		

		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

		if (keyguardManager.isKeyguardSecure()) {
			Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
				null, null
			);
			if (intent != null) {
				startActivityForResult(intent, 1337);
			}
		} else { 
			//No password on device. Pass. (Нет пароля на телефоне. Пропустим.)
			RESULT=true;
			setContentView(layout);
		}
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1337) {
			if (resultCode == RESULT_OK) {			
				RESULT=true;
				setContentView(layout);
			} else {
				finish();
			}
		}
	}

    private void initializeDefaultLayoutsIfNeeded(boolean isRussianDevice) {
        Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        boolean changed = false;
        if (!prefs.contains(KEY_LAYOUT_RU)) {

            String[][] russianLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"й","ц","у","к","е","н","г","ш","щ","з","х"},
                {"ф","ы","в","а","п","р","о","л","д","ж","э"},
                {"⇪","я","ч","с","м","и","т","ь","б","ю","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_RU, string2DArrayToJson(russianLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_EN)) {
            String[][] englishLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l"},
                {"⇪","z","x","c","v","b","n","m","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_EN, string2DArrayToJson(englishLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_SYM)) {
            String[][] symbolLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
		        {"/","\\","`","+","*","@","#","$","^","&","'"},
                {"=","|","<",">","[","]","(",")","{","}","\""},
                {"😃","~","%","-","—","_",":",";","!","?","⌫"},
                {"abc","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_SYM, string2DArrayToJson(symbolLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_EMOJI)) {
            String[][] emojiLetters = {
                {"😀","😢","😡","🤡","💩","👍","😭","🤬","😵","☠️","😄"},
                {"😁","😔","😤","😜","🤢","😆","😟","😠","😝","🤮","👎"},
                {"😂","😞","😣","😛","😷","🤣","🥰","😖","🤨","🤒","🤧"},
                {"!#?","😊","😫","🧐","🥴","💔","☹️","😩","🐷","😵‍💫","⌫"},
			    {"abc","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_EMOJI, string2DArrayToJson(emojiLetters));
            changed = true;
        }
        if (!prefs.contains(KEY_LAYOUT_ES)) {

            String[][] spanishLetters = {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l","ñ"},
                {"⇪","z","x","c","v","b","n","m","⌫"},
                {"!#?","🌐",","," ",".","⏎"}
            };
            ed.putString(KEY_LAYOUT_ES, string2DArrayToJson(spanishLetters));
            changed = true;
        }
        if (changed) ed.apply();
    }

    private void initializeDefaultLanguageFlagsIfNeeded(boolean isRussianDevice) {
        Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        boolean changed = false;
        if (!prefs.contains(KEY_LANG_RU) && !prefs.contains(KEY_LANG_EN) && !prefs.contains(KEY_LANG_ES)
			&& !prefs.contains(KEY_LANG_SYM) && !prefs.contains(KEY_LANG_EMOJI)) {
            if (isRussianDevice) {
                ed.putBoolean(KEY_LANG_RU, true);
                ed.putBoolean(KEY_LANG_EN, true);
                ed.putBoolean(KEY_LANG_ES, false);
                ed.putBoolean(KEY_LANG_SYM, true);
                ed.putBoolean(KEY_LANG_EMOJI, true);
            } else {
                ed.putBoolean(KEY_LANG_RU, false);
                ed.putBoolean(KEY_LANG_EN, true);
                ed.putBoolean(KEY_LANG_ES, true);
                ed.putBoolean(KEY_LANG_SYM, true);
                ed.putBoolean(KEY_LANG_EMOJI, true);
            }
            changed = true;
        }
        if (changed) ed.apply();
    }

    private String string2DArrayToJson(String[][] arr) {
        JSONArray outer = new JSONArray();
        for (int i = 0; i < arr.length; i++) {
            JSONArray inner = new JSONArray();
            for (int j = 0; j < arr[i].length; j++) {
                inner.put(arr[i][j]);
            }
            outer.put(inner);
        }
        return outer.toString();
    }

    public static String getCustomCommand(Context context) {
        Context deviceProtectedContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_CUSTOM_COMMAND, "");
    }
}
