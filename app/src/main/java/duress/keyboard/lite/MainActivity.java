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

	private void showAlertSetPasswordPlease() {
	String currentLang = Locale.getDefault().getLanguage();
    String alertMessage;
    String buttonText;

    if ("ru".equals(currentLang)) {
        alertMessage = "Установите текстовый пароль чтобы иметь возможность включить этот режим";
        buttonText = "Ок";
    } else {
        alertMessage = "Please set a text password to be able to enable this mode";
        buttonText = "OK";
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(alertMessage);
    builder.setCancelable(false);
    
    builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivity(intent);
        }
    });

    AlertDialog dialog = builder.create();
    dialog.show();
    
    if (dialog.getWindow() != null) {
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setGravity(Gravity.CENTER);
        }

        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.gravity = Gravity.CENTER;
        lp.x = 0;
        lp.y = 0;
        dialog.getWindow().setAttributes(lp);
    }
	}
	
	private void showToastErrorPackage() {
	final String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);			
	if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) return;			   						    	
    String currentLang = Locale.getDefault().getLanguage();
    String alertMessage;
    String buttonText;

    if ("ru".equals(currentLang)) {
        alertMessage = "Ошибка получения пакета поля ввода пароля. Убедитесь что у вас стоит текстовый пароль и отключена биометрия. Если нет - это причина ошибки.";
        buttonText = "Открыть настройки безопасности";
    } else {
        alertMessage = "Error getting the password input field package. Make sure you have a text password and biometrics disabled. If not, this is the cause of the error.";
        buttonText = "Open security settings";
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(alertMessage);
    builder.setCancelable(false);
    
    builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
        }
    });

    AlertDialog dialog = builder.create();
    dialog.show();

    if (dialog.getWindow() != null) {
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setGravity(Gravity.CENTER);
        }

        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.gravity = Gravity.CENTER;
        lp.x = 0;
        lp.y = 0;
        dialog.getWindow().setAttributes(lp);
    }
	}
	

	public static volatile boolean isExecConfirm=false;	
	
	private static boolean main=true;	
	private AlertDialog deadHandDialog;
	private android.widget.Switch switchDH; 
    private static final String PREFS_NAME = "SimpleKeyboardPrefs";
    private static final String KEY_CUSTOM_COMMAND = "custom_wipe_command";
	private BroadcastReceiver screenOffReceiver;
	private static final String KEY_DEAD_HAND_MODE = "dead_hand_mode";
	
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

	private void openKeyboardSettings() {

	try { 
		Intent std = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);									
		startActivity(std); 
	    return;	
	} catch (Throwable t1) {}	
		
	try {	
        Intent intent = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$KeyboardSettingsActivity"));
        intent.putExtra(":settings:fragment_args_key", "virtual_keyboard_pref");    
        startActivity(intent);
		return;
    } catch (Throwable t2) {}

	try {	
        Intent internal = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$KeyboardSettingsActivity"));									
	    startActivity(internal);	
		return;
    } catch (Throwable t3) {}
				
	}	

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
		if (deadHandDialog != null) {
		   if (deadHandDialog.isShowing()) {
               deadHandDialog.dismiss();
		   }
		    deadHandDialog=null;	
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
					explanation = "Привет, это приложение DuressKeyboardLite. Оно стирает данные при вводе задаваемого вами кода сброса через его клавиатуру на экране блокировки и нажатии стрелки Enter (⏎). Также имеет другие функции сброса данных. Дайте права Администратора для их работы.";
				} else {
					explanation = "Hi, this is the DuressKeyboardLite app. It wipes phone data upon entering the reset code through it on the lock screen and pressing the Enter arrow (⏎). It also has other data wipe features. Give Administrator rights for their work.";
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
					if (cmd.length() < 4) {
                    Toast.makeText(MainActivity.this,
                    isRussianDevice ? "Минимум 4 символа" : "Minimum 4 characters required",
                    Toast.LENGTH_SHORT).show();
                    return;
                    }
					
					if (!cmd.isEmpty()) {
						try {

							String salt = generateSalt();
							String commandHash = hashKeyWithSalt(salt, cmd);


							Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
							SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

							prefs.edit()
								.putString(KEY_CUSTOM_COMMAND, commandHash)
								.putString("command_salt", salt)
								.commit();


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


				private static final String in_ru="Привет. Это приложение DuressKeybordLite, созданное для защиты личных данных. В нём вы можете задать код сброса (Duress Password). Он сотрёт данные при его вводе на экране блокировки и нажатии стрелки ввода (⏎). Для этого у вас на телефоне должен быть установлен буквенный пароль, а код сброса должен отличаться от него, и клавиатура должна быть включена и выбрана по умолчанию на постоянной основе, тогда она будет работать как при обычном использовании, так и на экране блокировки на всех Android за исключением некоторых китайских прошивок (Realme, Oppo, ...)\n\nЭто клавиатура созданна для повседневнего использования и для постоянной готовности к ситуациям, когда вас могут заставить разблокировать телефон против вашей воли. Именно тогда вы вводите код сброса (DuressPassword) вместо обычного пароля и он делает сброс телефона до заводских настроек.\n\nДумаете что оно бесполезно? Вас могут заставить разблокировать телефон когда вы просто гуляли в парке, или шли в магазин. Такое происходит всё чаще. Если вы дадите посторонним свои личные данные даже под предлогом простой проверки, кем бы они не представлялись, их могут использовать для шантажа, а вы даже не сможете обратиться за помощью из-за опасения утечки со стороны вымогателей. Если вы просто сотрёте данные ещё до их предоставления, у злоумышленников больше не будет рычагов давления на вас.\n\nДополнительные функции:\n\nУстановка лимита попыток ввода пароля (настройки Авто-Сброса). Позволяет установить лимит попыток ввода пароля, что означает что если вы допустите количество ошибочных попыток равное этому лимиту, то данные будут стерты также как и при вводе кода сброса. За такую попытку считается ввод пароля длинной больше 4х символов, где вы допустили хоть 1 ошибку, тоесть если он не соответствует вашему паролю разблокировки экрана.\n\nПриложение также имеет режим Мёртвой Руки, который можно включить в главном меню. Он нужен чтобы предотвратить сценарий когда вас заставляют переключиться на другой способ ввода чтобы не дать вам возможность ввести код сброса. Он использует логику автоматического управления лимитом попыток, устанавливая его минимальное значение в 1, при этом когда клавиатура используется, увеличивает его перед каждой попыткой вплоть до 5. Это значит что когда вас заставляют обойти клавиатуру, вы можете просто ввести любой неверный пароль больше 4х символов для сброса данных, также как это делает код сброса, ведь у вас только 1 попытка. Пока вы используете клавиатуру, она защищает вас от случайных ошибок давая вам 5 попыток (прибавляя по 2 от текущих, перед отправкой ввода пока вы не достигните лимита в 5).\n\nИсходный код:\nhttps://github.com/pofesk0/DuressKeyboardLite/\nF-droid:\nhttps://f-droid.org/packages/duress.keyboard.lite/\n\nБольше функций в полной версии:\nhttps://f-droid.org/packages/duress.keyboard/\nЕё установка может быть сложнее. Подробнее:\nЗеркало 1:\nhttps://github.com/pofesk0/DuressKeyboard/\nЗеркало 2:\nhttps://github.com/pofesk0/lastcodeduresskeyboard/";
			    private static final String in_en="Hello. This is the DuressKeyboardLite application, created to protect personal data. In it you can set a reset code (Duress Password). It will erase data upon its entry on the lock screen and pressing the enter arrow (⏎). For this you must have a letter password installed on your phone, and the reset code must differ from it, and the keyboard must be enabled and selected by default on a permanent basis, then it will work both during normal use and on the lock screen on all Android except some Chinese firmware (Realme, Oppo, ...)\n\nThis keyboard is created for everyday use and for constant readiness for situations when you may be forced to unlock the phone against your will. It is then that you enter the reset code (DuressPassword) instead of the usual password and it resets the phone to factory settings.\n\nDo you think it is useless? You may be forced to unlock the phone when you were just walking in the park, or going to the store. This happens more and more often. If you give your personal data to strangers even under the pretext of simple verification, whoever they pretend to be, they can be used for blackmail, and you won't even be able to seek help due to fear of leakage on the part of extortionists. If you simply erase the data even before providing it, bad actors will no longer have leverage over you.\n\nAdditional features:\n\nSetting a limit on password entry attempts (Auto-Wipe settings). Allows to set a limit on password entry attempts, which means that if you make a number of failed attempts equal to this limit, the data will be erased as well as when entering the reset code. Such an attempt is considered to be entering a password longer than 4 characters, where you made at least 1 error, i.e. if it does not correspond to your screen unlock password.\n\nThe application also has a Dead Hand mode, which can be enabled in the main menu. It is needed to prevent a scenario where you are forced to switch to another input method to prevent you from entering the reset code. It uses automatic limit control logic, setting its minimum value to 1, while when the keyboard is used, increasing it before each attempt up to 5. This means that when you are forced to bypass the keyboard, you can simply enter any wrong password longer than 4 characters to reset the data, just as the reset code does, since you have only 1 attempt. While you are using the keyboard, it protects you from accidental errors giving you 5 attempts (adding 2 from current ones, before sending input until you reach the limit of 5).\n\nSource code:\nhttps://github.com/pofesk0/DuressKeyboardLite/\nF-droid:\nhttps://f-droid.org/packages/duress.keyboard.lite/\n\nMore functions in the full version:\nhttps://f-droid.org/packages/duress.keyboard/\nIts installation may be more difficult. Details:\nMirror 1:\nhttps://github.com/pofesk0/DuressKeyboard/ \nMirror 2:\nhttps://github.com/pofesk0/lastcodeduresskeyboard/​";
			
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
		keyboardSettingsButton.setText(isRussianDevice ? "Открыть настройки клавиатур чтобы включить DuressKeyboardLite" : "Open keyboard settings to enable DuressKeyboardLite");
		keyboardSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openKeyboardSettings();
				}
			});


		final Button chooseKeyboardButton = new Button(this);
		chooseKeyboardButton.setText(isRussianDevice ? "Выбрать DuressKeyboardLite если включена" : "Choose DuressKeyboardLite if enabled");
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

		
		final Button AutoWipeSettingsButton = new Button(this);
		AutoWipeSettingsButton.setText(isRussianDevice ? "Настройки Авто-Сброса" :
									  "Auto-wipe Settings");
		AutoWipeSettingsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
							Intent intent7a = new Intent(getApplicationContext(), AdditionalOptionsActivity.class);
							intent7a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent7a);
					} catch (Throwable ignored) {}
				}
			});		

		Context dpContext = getApplicationContext().createDeviceProtectedStorageContext();
		final SharedPreferences prefsDH = dpContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		
		switchDH = new android.widget.Switch(this);
		switchDH.setText(isRussianDevice ? "Режим Мертвой руки" : "Dead Hand Mode");
		switchDH.setTextSize(16);
		switchDH.setChecked(prefsDH.getBoolean(KEY_DEAD_HAND_MODE, false));

		 switchDH.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {	
		 KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

		if (keyguardManager!=null && keyguardManager.isKeyguardSecure()) {
			Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
				null, null
			);
			if (intent != null) {
				isExecConfirm=true;
				startActivityForResult(intent, 67);
			}
		} else {
		showAlertSetPasswordPlease();
		return;	
		}
			 
         if (deadHandDialog != null && deadHandDialog.isShowing()) {
            deadHandDialog.dismiss();
        }		

		final boolean isChecked = switchDH.isChecked();
        final boolean isRu = "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());
        float density = getResources().getDisplayMetrics().density;
        int p16 = (int) (16 * density + 0.5f);
        int p12 = (int) (12 * density + 0.5f);

        LinearLayout root = new LinearLayout(MainActivity.this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p16, p16, p16, p16);

        TextView messageText = new TextView(MainActivity.this);
        messageText.setTextSize(14);
        String titleE;
		if (!isChecked) {
			titleE = isRu ? "Отключить Режим Мертвой руки" : "Disable Dead Hand Mode";
            messageText.setText(isRu 
                ? "Вы уверены что хотите отключить режим мертвой руки? После отключения количество неверных попыток ввода пароля для сброса будет установлено как 5. Вы сможете изменить его в любой момент в настройках Авто-Сброса." 
                : "Are you sure you want to disable Dead Hand Mode? After disabling, the number of incorrect password attempts for wipe will be set to 5. You'll be able to change it at any time in the Auto-Wipe settings.");
        } else {
			titleE = isRu ? "Включить Режим Мертвой руки" : "Enable Dead Hand Mode";            
            messageText.setText(isRu 
                ? "Хотите включить режим мертвой руки?\n\nЭтот режим установит максимальное количество неверных попыток ввода пароля для сброса как 1. Пока используется данная клавиатура, это количество будет сдвигаться вплоть до 5 после ввода пароля перед отправкой, если это не DuressPassword, a после нее сразу заново устанавливаться как 1.\n\nЭто значит, что если кто-то заставит вас ввести пароль в обход клавиатуры, или если система запретит использование клавитуры на экране блокировки, вы всё равно будете защищены: будет достаточно один раз ввести неверный пароль длиннее 4х символов чтобы стереть все данные, по сути, сделав то же самое что делает DuressPassword. А пока вы используете клавиатуру у вас фактически 5 попыток." 								
                : "Want to enable Dead Hand Mode?\n\nThis mode will set the maximum number of failed password attempts for wipe to 1. While this keyboard is in use, this count will be shifted up to 5 after entering the password before sending it if this is not DuressPassword, and after sending it will immediately set it back to 1.\n\nThis means if someone forces you to enter password bypassing keyboard, or if system restricts keyboard usage on lock screen, you are still protected: only need to enter wrong password longer than 4 characters once to wipe all data, essentially doing the same as DuressPassword. But while you're using the keyboard, you actually have 5 attempts.");
			String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
			if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) {
				titleE = isRu ? "Ошибка" : "Error";            
                messageText.setText(isRu 
                ? "Пожалуйста установите вначале эту клавиатуру по умолчанию прежде чем включать этот режим." : 
				"Please, set this keyboard by default before enabling this mode.");
			}			
			
        }
        
        
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textLp.bottomMargin = p16;
        root.addView(messageText, textLp);

        LinearLayout buttonsLayout = new LinearLayout(MainActivity.this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.END);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btnCancel = new Button(MainActivity.this);
        btnCancel.setText(isRu ? "Отмена" : "Cancel");
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchDH.setChecked(!isChecked);
                if (deadHandDialog != null) deadHandDialog.dismiss();
            }
        });
        buttonsLayout.addView(btnCancel, btnLp);

        View spacer = new View(MainActivity.this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(p12, 1);
        buttonsLayout.addView(spacer, spacerLp);

        Button btnAction = new Button(MainActivity.this);		
        btnAction.setText(isChecked ? (isRu ? "Включить" : "Enable") : (isRu ? "Выключить" : "Disable"));
		final String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
		if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) {
			btnAction.setText(isChecked ? (isRu ? "Настройки клавиатур" : "Keyboard settings") : (isRu ? "Выключить" : "Disable"));		
		}					 	 
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				if (defaultIme == null || !defaultIme.startsWith(getPackageName() + "/")) {			   
					if (isChecked) {
					openKeyboardSettings();		
					switchDH.setChecked(false);	
					if (deadHandDialog != null) deadHandDialog.dismiss();						
					return;	
					}
				}			
                prefsDH.edit().putBoolean(KEY_DEAD_HAND_MODE, isChecked).apply();
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName adminComponent = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.setMaximumFailedPasswordsForWipe(adminComponent, isChecked ? 1 : 5);
                }
                if (deadHandDialog != null) deadHandDialog.dismiss();
            }
        });
        buttonsLayout.addView(btnAction, btnLp);

        root.addView(buttonsLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        deadHandDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(titleE)
                .setView(root)
                .setCancelable(false)
                .create();

        deadHandDialog.show();

        Window window = deadHandDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp2 = window.getAttributes();
            lp2.gravity = Gravity.CENTER;
            lp2.x = 0;
            lp2.y = 0;
            window.setAttributes(lp2);
        }
    } });

        

		layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(commandInput);
        layout.addView(saveButton);
		layout.addView(keyboardSettingsButton);
		layout.addView(chooseKeyboardButton);
        layout.addView(selectLanguagesButton);
		layout.addView(readInstructionsButton);
		layout.addView(AutoWipeSettingsButton);
		layout.addView(switchDH);
		

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

		if (requestCode == 67) {
			isExecConfirm=false;
			if (resultCode != RESULT_OK) {				
			finish();
			}
			Context deviceProtectedContext = getApplicationContext().createDeviceProtectedStorageContext();
			SharedPreferences prefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);				
			String next = prefs.getString("key_field_pac", "Error, no value");
			if (next.equals("Error, no value")) showToastErrorPackage();
		}

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
