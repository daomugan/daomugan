package com.rd.callcar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.adapter.HistoryAdapter;
import com.rd.callcar.entity.CallHistory;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class CallRecord extends Activity implements OnScrollListener {

	final int LOGINSUCCESS_MSG = 0;
	final int LOGINFAIL_MSG = 1;
	public final int getMoreSuccess = 2;
	public final int getMoreFail = 3;
	App app = null;

	private Button back;
	private ListView historyList;

	private List<CallHistory> list = new ArrayList<CallHistory>();
	int page = 1;
	private int visibleLastIndex = 0;
	private boolean isLoad = true;
	HistoryAdapter adapter;
	private View footer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) getApplication();

		back = (Button) findViewById(R.id.back);
		historyList = (ListView) findViewById(R.id.historyList);
		footer = getLayoutInflater().inflate(R.layout.listfooter, null);
		historyList.addFooterView(footer);
		adapter = new HistoryAdapter(CallRecord.this, list);
		historyList.setAdapter(adapter);
		historyList.setOnScrollListener(this);
		historyList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				Bundle bundle = new Bundle();
				bundle.putSerializable("data", (Serializable) list);

				startActivity(new Intent(CallRecord.this,
						ComplantActivity.class).putExtras(bundle));
			}
		});

		back = (Button) findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CallRecord.this.finish();
			}
		});

		LoadHistory();
	}

	private void LoadHistory() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					List<CallHistory> list = getJson.getRecordList(
							app.getUSerid(), page);
					if (list != null && list.size() > 0) {
						Message message = new Message();
						message.what = LOGINSUCCESS_MSG;
						message.obj = list;
						mhandler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = LOGINFAIL_MSG;
						mhandler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = LOGINFAIL_MSG;
					mhandler.sendMessage(message);
				}
			}
		}).start();
	}

	// 线程处理
	private Handler mhandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOGINSUCCESS_MSG:
				list.addAll((List<CallHistory>) msg.obj);
				if (historyList.getFooterViewsCount() > 0)
					historyList.removeFooterView(footer);
				adapter.notifyDataSetChanged();
				break;
			case LOGINFAIL_MSG:
				TextView tt = (TextView) footer
						.findViewById(R.id.footer_loading);
				tt.setText(R.string.historyFail);
				break;
			case getMoreFail:
				isLoad = true;
				TextView tt1 = (TextView) footer
						.findViewById(R.id.footer_loading);
				tt1.setText(R.string.noMorehistory);
				break;
			case getMoreSuccess:
				isLoad = true;
				list.addAll((List<CallHistory>) msg.obj);
				if (historyList.getFooterViewsCount() > 0)
					historyList.removeFooterView(footer);
				adapter.notifyDataSetChanged();
				break;
			}
		}
	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		visibleLastIndex = firstVisibleItem + visibleItemCount - 1;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		int itemsLastIndex = adapter.getCount() - 1; // 数据集最后一项的索引
		int lastIndex = itemsLastIndex + 1;
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& visibleLastIndex == lastIndex - 1 && isLoad) {
			isLoad = false;
			LoadMore();
		}
	}

	private void LoadMore() {
		page++;
		TextView tt = (TextView) footer.findViewById(R.id.footer_loading);
		tt.setText(R.string.waiting);
		historyList.addFooterView(footer);
		new Thread(new Runnable() {

			public void run() {
				try {
					List<CallHistory> listData = getJson.getRecordList(
							app.getUSerid(), page);
					if (listData != null && listData.size() > 0) {
						Message msg = new Message();
						msg.what = getMoreSuccess;
						msg.obj = listData;
						mhandler.sendMessage(msg);
					} else {
						Message msg = new Message();
						msg.what = getMoreFail;
						mhandler.sendMessage(msg);
					}
				} catch (Exception e) {
					Message msg = new Message();
					msg.what = getMoreFail;
					mhandler.sendMessage(msg);
				}
			}
		}).start();
	}
}


package com.rd.callcar;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class CallSuccess extends Activity {

	private Button coutinue, cancel, back;

	private ProgressDialog mpDialog;

	final int LOGINSUCCESS_MSG = 0;
	final int LOGINFAIL_MSG = 1;

	App app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.success_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) this.getApplication();

		// 初始化加载对话框
		mpDialog = new ProgressDialog(this);
		mpDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mpDialog.setTitle(R.string.loading_data);
		mpDialog.setIcon(android.R.drawable.ic_dialog_info);
		mpDialog.setMessage(getString(R.string.canceling));
		mpDialog.setIndeterminate(false);
		mpDialog.setCancelable(true);

		coutinue = (Button) findViewById(R.id.coutinue);
		back = (Button) findViewById(R.id.back);

		cancel = (Button) findViewById(R.id.cancel);

		coutinue.setOnClickListener(new OnClk());
		back.setOnClickListener(new OnClk());
		cancel.setOnClickListener(new OnClk());
	}

	class OnClk implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.coutinue:
			case R.id.back:
				CallSuccess.this.finish();
				break;
			case R.id.cancel:
				Cancel();
				break;
			}
		}
	}

	private void Cancel() {
		mpDialog.show();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					boolean isCancel = getJson.cancelCall(app.getUSerid(),
							getSave("backId"));
					if (isCancel) {
						Message message = new Message();
						message.what = LOGINSUCCESS_MSG;
						mhandler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = LOGINFAIL_MSG;
						mhandler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = LOGINFAIL_MSG;
					mhandler.sendMessage(message);
				}
			}
		}).start();
	}

	// 线程处理
	private Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mpDialog.dismiss();
			switch (msg.what) {
			case LOGINSUCCESS_MSG:
				ShowToast("取消打车成功！");
				startActivity(new Intent(CallSuccess.this, StepOne.class));
				break;
			case LOGINFAIL_MSG:
				ShowToast("取消打车失败！");
				break;
			}
		}
	};

	private String getSave(String key) {
		return PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(key, "");
	}

	private void ShowToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}


package com.rd.callcar;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.entity.FeedMsg;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CompantEnterActivity extends Activity {

	private EditText feedmessage, contact;
	private Button back, clear, submit_feed;

	private ProgressDialog pro = null;

	private final int feedkbackSuccess = 0;
	final int fedkbackFail = 1;
	App app = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.enter_layout);
		ExitApplication.getInstance().addActivity(this);

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		app = (App) getApplication();

		// 初始化加载对话框
		pro = new ProgressDialog(this);
		pro.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pro.setTitle("提示");
		pro.setIcon(android.R.drawable.ic_dialog_info);
		pro.setMessage("正在提交反馈，请稍候……");
		pro.setIndeterminate(false);
		pro.setCancelable(true);

		feedmessage = (EditText) findViewById(R.id.feedmessage);
		contact = (EditText) findViewById(R.id.contact);

		back = (Button) findViewById(R.id.back);
		clear = (Button) findViewById(R.id.clear);
		submit_feed = (Button) findViewById(R.id.submit_feed);

		back.setOnClickListener(new OnClk());
		clear.setOnClickListener(new OnClk());
		submit_feed.setOnClickListener(new OnClk());
	}

	class OnClk implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.clear:
				if (feedmessage.isFocused())
					feedmessage.setText("");
				if (contact.isFocused())
					contact.setText("");
				break;
			case R.id.submit_feed:
				SubMit();
				break;
			case R.id.back:
				CompantEnterActivity.this.finish();
				break;
			}
		}
	}

	private void Clear() {
		feedmessage.setText("");
		contact.setText("");
	}

	private void SubMit() {
		String msg = feedmessage.getText().toString().trim();
		String add = contact.getText().toString().trim();

		if (msg.equals("")) {
			ShowToast("请输入反馈内容！");
			return;
		}

		if (msg.length() > 500) {
			ShowToast("输入内容过多，请删减部分内容！");
			return;
		}

		if (add.length() > 200) {
			ShowToast("联系方式输入过长，请修改长度！");
			return;
		}

		final FeedMsg fedmsg = new FeedMsg();
		fedmsg.setUserid(app.getUSerid());
		fedmsg.setContent(msg);
		fedmsg.setAddress(add);

		pro.show();
		new Thread(new Runnable() {

			public void run() {
				try {
					boolean isSub = getJson.SubFeedBack(fedmsg);
					if (isSub) {
						Message message = new Message();
						message.what = feedkbackSuccess;
						handler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = fedkbackFail;
						handler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = fedkbackFail;
					handler.sendMessage(message);
				}
			}
		}).start();
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			pro.dismiss();
			switch (msg.what) {
			case feedkbackSuccess:
				ShowToast("反馈成功，感谢您的反馈！");
				Clear();
				break;
			case fedkbackFail:
				ShowToast("反馈失败，请检测网络或者重新提交，感谢您的使用！");
				break;
			}
		}
	};

	private void ShowToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}


package com.rd.callcar;

import java.util.ArrayList;
import java.util.List;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.adapter.ComplantAdapter;
import com.rd.callcar.entity.CallHistory;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class ComplantActivity extends Activity {

	private List<CallHistory> list=new ArrayList<CallHistory>();
	private Button back, button_say, button_complant;
	private ListView complantList;

	private ProgressDialog pro = null;

	private final int feedkbackSuccess = 0;
	final int fedkbackFail = 1;
	App app = null;
	ComplantAdapter adapter;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.complant_layout);
		ExitApplication.getInstance().addActivity(this);

	//	list = (List<CallHistory>) getIntent().getSerializableExtra("data");
		CallHistory history = new CallHistory();
		history.setsituation("您好，我在你的附近，我的车正好与您同路，您可以搭载我的顺风车！");
        list.add(history);
		app = (App) getApplication();

		// 初始化加载对话框
		pro = new ProgressDialog(this);
		pro.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pro.setTitle("提示");
		pro.setIcon(android.R.drawable.ic_dialog_info);
		pro.setMessage("正在提交，请稍候……");
		pro.setIndeterminate(false);
		pro.setCancelable(true);

		complantList = (ListView) findViewById(R.id.complantList);
		adapter = new ComplantAdapter(this, list);
		complantList.setAdapter(adapter);
		complantList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				adapter.CheckOnlyOne(arg2);
			}
		});

		back = (Button) findViewById(R.id.back);
		button_say = (Button) findViewById(R.id.button_say);
		button_complant = (Button) findViewById(R.id.button_complant);

		back.setOnClickListener(new OnClk());
		button_say.setOnClickListener(new OnClk());
		button_complant.setOnClickListener(new OnClk());
	}

	class OnClk implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back:
				ComplantActivity.this.finish();
				break;
			case R.id.button_say:
				startActivity(new Intent(ComplantActivity.this,
						CompantEnterActivity.class));
				break;
			case R.id.button_complant:
				ShowDD();
				break;
			}
		}
	}

	private void ShowDD() {
		if (adapter.getSelect() == null) {
			app.ShowToast("请选择一个发布内容！");
			return;
		}

		final AlertDialog alertDialog = new AlertDialog.Builder(
				ComplantActivity.this)
				.setTitle(R.string.Hint)
				.setMessage("您确定要进行发布吗？")
				.setPositiveButton(R.string.sure,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								Complant();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create();
		alertDialog.show();
	}

	private void Complant() {
		pro.show();
		new Thread(new Runnable() {

			public void run() {
				try {
					boolean isSub = getJson.isComplantSelect(app.getUSerid(),
							adapter.getSelect().getSeqid());
					if (isSub) {
						Message message = new Message();
						message.what = feedkbackSuccess;
						handler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = fedkbackFail;
						handler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = fedkbackFail;
					handler.sendMessage(message);
				}
			}
		}).start();
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			pro.dismiss();
			switch (msg.what) {
			case feedkbackSuccess:
				app.ShowToast("发布成功！");
				break;
			case fedkbackFail:
				app.ShowToast("发布失败！");
				break;
			}
		}
	};
}


package com.rd.callcar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.adapter.HistoryAdapter;
import com.rd.callcar.entity.CallHistory;
import com.rd.callcar.entity.PointInfo;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class Driver extends Activity implements OnScrollListener {

	final int LOGINSUCCESS_MSG = 0;
	final int LOGINFAIL_MSG = 1;
	public final int getMoreSuccess = 2;
	public final int getMoreFail = 3;
	App app = null;

	private Button back;
	private ListView historyList;

	private List<CallHistory> list = new ArrayList<CallHistory>();
	int page = 1;
	private int visibleLastIndex = 0;
	private boolean isLoad = true;
	HistoryAdapter adapter;
	private View footer;
	final PointInfo info = new PointInfo();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.driver_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) getApplication();

		back = (Button) findViewById(R.id.back1);
		historyList = (ListView) findViewById(R.id.historyList1);
		footer = getLayoutInflater().inflate(R.layout.listfooter, null);
		historyList.addFooterView(footer);
		adapter = new HistoryAdapter(Driver.this, list);
		historyList.setAdapter(adapter);
		historyList.setOnScrollListener(this);
		historyList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				Bundle bundle = new Bundle();
				bundle.putSerializable("data", (Serializable) list);

				startActivity(new Intent(Driver.this,
						ComplantActivity.class).putExtras(bundle));
			}
		});

		back = (Button) findViewById(R.id.back1);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Driver.this.finish();
			}
		});

		LoadHistory();
	}

	private void LoadHistory() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
				
					info.setUserid(app.getUSerid());
					info.setCity(getSave("city"));
					info.setLang(getSave("lang"));
					info.setLant(getSave("lant"));
					info.setType(getSave("type"));
					info.setDestnation(getSave("key"));
					List<CallHistory> list = getJson.DrivergetList(
							info, page);
					if (list != null && list.size() > 0) {
						Message message = new Message();
						message.what = LOGINSUCCESS_MSG;
						message.obj = list;
						mhandler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = LOGINFAIL_MSG;
						mhandler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = LOGINFAIL_MSG;
					mhandler.sendMessage(message);
				}
			}
		}).start();
	}

	// 线程处理
	private Handler mhandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOGINSUCCESS_MSG:
				list.addAll((List<CallHistory>) msg.obj);
				if (historyList.getFooterViewsCount() > 0)
					historyList.removeFooterView(footer);
				adapter.notifyDataSetChanged();
				break;
			case LOGINFAIL_MSG:
				TextView tt = (TextView) footer
						.findViewById(R.id.footer_loading);
				tt.setText(R.string.historyFail);
				break;
			case getMoreFail:
				isLoad = true;
				TextView tt1 = (TextView) footer
						.findViewById(R.id.footer_loading);
				tt1.setText(R.string.noMorehistory);
				break;
			case getMoreSuccess:
				isLoad = true;
				list.addAll((List<CallHistory>) msg.obj);
				if (historyList.getFooterViewsCount() > 0)
					historyList.removeFooterView(footer);
				adapter.notifyDataSetChanged();
				break;
			}
		}
	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		visibleLastIndex = firstVisibleItem + visibleItemCount - 1;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		int itemsLastIndex = adapter.getCount() - 1; // 数据集最后一项的索引
		int lastIndex = itemsLastIndex + 1;
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& visibleLastIndex == lastIndex - 1 && isLoad) {
			isLoad = false;
			LoadMore();
		}
	}
	private String getSave(String key) {
		return PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(key, "");
	}
	private void LoadMore() {
		page++;
		TextView tt = (TextView) footer.findViewById(R.id.footer_loading);
		tt.setText(R.string.waiting);
		historyList.addFooterView(footer);
		
		new Thread(new Runnable() {

			public void run() {
				try {
					List<CallHistory> listData = getJson.DrivergetList(
							info, page);
					if (listData != null && listData.size() > 0) {
						Message msg = new Message();
						msg.what = getMoreSuccess;
						msg.obj = listData;
						mhandler.sendMessage(msg);
					} else {
						Message msg = new Message();
						msg.what = getMoreFail;
						mhandler.sendMessage(msg);
					}
				} catch (Exception e) {
					Message msg = new Message();
					msg.what = getMoreFail;
					mhandler.sendMessage(msg);
				}
			}
		}).start();
	}
}


package com.rd.callcar;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends Activity {

	private Button login, register;
	private EditText input_userid, input_pwd;

	private ProgressDialog mpDialog;

	final int LOGINSUCCESS_MSG = 0;
	final int LOGINFAIL_MSG = 1;
	App app = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) getApplication();

//		if (app.getLogin()) {
//			StartMain();
//		}

		// 初始化加载对话框
		mpDialog = new ProgressDialog(this);
		mpDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mpDialog.setTitle(R.string.loading_data);
		mpDialog.setIcon(android.R.drawable.ic_dialog_info);
		mpDialog.setMessage(getString(R.string.waiting));
		mpDialog.setIndeterminate(false);
		mpDialog.setCancelable(true);

		login = (Button) findViewById(R.id.login);
		register = (Button) findViewById(R.id.register);

		input_userid = (EditText) findViewById(R.id.input_userid);
		input_pwd = (EditText) findViewById(R.id.input_pwd);

		login.setOnClickListener(new OnClick());
		register.setOnClickListener(new OnClick());
	}

	class OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.login:
				LoginMeth();
				break;
			case R.id.register:
				startActivityForResult(new Intent(Login.this, Register.class),
						1);
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == 1) {
				input_userid.setText(data.getStringExtra("userid"));
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void LoginMeth() {
		final String userid = input_userid.getText().toString().trim();
		final String pwd = input_pwd.getText().toString().trim();

		if (userid.equals("")) {
			ShowToast(R.string.noUserNameErr);
			return;
		}

		if (pwd.equals("")) {
			ShowToast(R.string.noPwdErr);
			return;
		}

		mpDialog.show();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					boolean isLogin = getJson.Login(userid, pwd);
					if (isLogin) {
						Message message = new Message();
						message.what = LOGINSUCCESS_MSG;
						message.obj = userid;
						mhandler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = LOGINFAIL_MSG;
						mhandler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = LOGINFAIL_MSG;
					mhandler.sendMessage(message);
				}
			}
		}).start();
	}

	// 线程处理
	private Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mpDialog.dismiss();
			switch (msg.what) {
			case LOGINSUCCESS_MSG:
				app.SaveBegin("userid", input_userid.getText().toString().trim());
				app.SaveLogin(true);
				StartMain();
				break;
			case LOGINFAIL_MSG:
				ShowToast(R.string.loginFail);
				break;
			}
		}
	};

	private void StartMain() {
		startActivity(new Intent(Login.this, StepOne.class));
		Login.this.finish();
	}

	private void ShowToast(int res) {
		Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
	}
}


package com.rd.callcar;

import com.rd.callcar.Util.DialogManager;
import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.Util.UpdateCustomer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	Button button_set, button_record,button_zhaobiao,button_toubiao;
	App app = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) getApplication();

		new UpdateCustomer(this, false).Lgoining();

		button_set = (Button) findViewById(R.id.button_set);
		button_record = (Button) findViewById(R.id.button_record);
		button_zhaobiao = (Button) findViewById(R.id.button_zhaobiao);
		button_toubiao = (Button) findViewById(R.id.button_toubiao);

		button_set.setOnClickListener(new OnClk());
		button_record.setOnClickListener(new OnClk());
	}

	class OnClk implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_set:
				if (app.getLogin()) {
					startActivity(new Intent(MainActivity.this,
							Driver.class));
				} else
				
				{
					
					startActivity(new Intent(MainActivity.this, Login.class));
				}
				break;
			case R.id.button_record:
				if (app.getLogin()) {
					startActivity(new Intent(MainActivity.this,
							StepOne.class));
				} else {
					startActivity(new Intent(MainActivity.this, Login.class));
				}
				break;
			case R.id.button_zhaobiao:
				if (app.getLogin()) {
					startActivity(new Intent(MainActivity.this,
							ComplantActivity.class));
				}
				else 
				{
					startActivity(new Intent(MainActivity.this, Login.class));
				}
				break;
			case R.id.button_toubiao:
				if (app.getLogin()) {
					startActivity(new Intent(MainActivity.this,
							ComplantActivity.class));
				}
				else 
				{
					startActivity(new Intent(MainActivity.this, Login.class));
				}
				break;
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new DialogManager().showExitHit(this);
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
}


package com.rd.callcar;

import com.rd.callcar.Util.ExitApplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class NameSettingActivity extends Activity {

	private Button back, save;
	private EditText xingEdit, mingEdit, nichenEdit;
	private CheckBox showTrueName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.name_layout);
		ExitApplication.getInstance().addActivity(this);

		back = (Button) findViewById(R.id.back);
		save = (Button) findViewById(R.id.save);

		xingEdit = (EditText) findViewById(R.id.xingEdit);
		mingEdit = (EditText) findViewById(R.id.mingEdit);
		nichenEdit = (EditText) findViewById(R.id.nichenEdit);

		showTrueName = (CheckBox) findViewById(R.id.showTrueName);

		readSave();

		back.setOnClickListener(new OnClk());
		save.setOnClickListener(new OnClk());
	}

	private void readSave() {
		showTrueName.setChecked(PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getBoolean("showreadName", false));

		xingEdit.setText(PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("xing", ""));

		mingEdit.setText(PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("ming", ""));

		nichenEdit.setText(PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("nichen", ""));
	}

	class OnClk implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back:
				NameSettingActivity.this.finish();
				break;
			case R.id.save:
				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putBoolean("showreadName", showTrueName.isChecked())
						.commit();

				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putString("xing", xingEdit.getText().toString())
						.commit();

				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putString("ming", mingEdit.getText().toString())
						.commit();

				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putString("nichen", nichenEdit.getText().toString())
						.commit();

				Toast.makeText(NameSettingActivity.this, "保存成功！",
						Toast.LENGTH_SHORT).show();
				String name = showTrueName.isChecked() ? nichenEdit.getText()
						.toString() : xingEdit.getText().toString()
						+ mingEdit.getText().toString();

				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit().putString("name", name).commit();
				NameSettingActivity.this.setResult(1,
						new Intent().putExtra("name", name));
				NameSettingActivity.this.finish();
				break;
			}
		}
	}

}


package com.rd.callcar;

import com.rd.callcar.Util.ExitApplication;
import com.rd.callcar.json.getJson;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PwdSettingActivity extends Activity {

	private EditText oldPwdEdit, newPwdEdit, newsurePwdEdit;
	private Button save, back;

	App app = null;

	final int ChangePwdSUCCESS_MSG = 0;
	final int ChangePwdFAIL_MSG = 1;

	private ProgressDialog mpDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pwd_layout);
		ExitApplication.getInstance().addActivity(this);

		app = (App) getApplication();

		// 初始化加载对话框
		mpDialog = new ProgressDialog(this);
		mpDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mpDialog.setTitle(R.string.pwdHint);
		mpDialog.setIcon(android.R.drawable.ic_dialog_info);
		mpDialog.setMessage(getString(R.string.saving));
		mpDialog.setIndeterminate(false);
		mpDialog.setCancelable(true);

		oldPwdEdit = (EditText) findViewById(R.id.oldPwdEdit);
		newPwdEdit = (EditText) findViewById(R.id.newPwdEdit);
		newsurePwdEdit = (EditText) findViewById(R.id.newsurePwdEdit);

		save = (Button) findViewById(R.id.save);
		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ChangePwd();
			}
		});

		back = (Button) findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				PwdSettingActivity.this.finish();
			}
		});
	}

	private void ChangePwd() {
		final String oldPwd = oldPwdEdit.getText().toString().trim();
		final String newPwd = newPwdEdit.getText().toString().trim();
		String newPwdSure = newsurePwdEdit.getText().toString().trim();

		if (oldPwd.equals("")) {
			app.ShowToast(R.string.enterOldPwdHint);
			return;
		}

		if (newPwd.equals("")) {
			app.ShowToast(R.string.enterNewOwdHint);
			return;
		}

		if (!newPwdSure.equals(newPwd)) {
			app.ShowToast(R.string.pwdNoEqualHint);
			return;
		}

		if (newPwd.length() < 6) {
			app.ShowToast(R.string.noEnough);
			return;
		}

		if (newPwd.length() > 18) {
			app.ShowToast(R.string.noMore);
			return;
		}

//		if (!newPwd.matches("[a-zA-Z][a-zA-Z0-9]*")) {
//			app.ShowToast(R.string.pwdRegx);
//			return;
//		}

		final String userid = app.getUSerid();

		mpDialog.show();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String back = getJson.ChangePwd(userid, oldPwd, newPwd);
					if (back != null) {
						Message message = new Message();
						message.what = ChangePwdSUCCESS_MSG;
						message.obj = userid;
						mhandler.sendMessage(message);
					} else {
						Message message = new Message();
						message.what = ChangePwdFAIL_MSG;
						mhandler.sendMessage(message);
					}
				} catch (Exception e) {
					Message message = new Message();
					message.what = ChangePwdFAIL_MSG;
					mhandler.sendMessage(message);
				}
			}
		}).start();
	}

	// 线程处理
	private Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mpDialog.dismiss();
			switch (msg.what) {
			case ChangePwdSUCCESS_MSG:
				app.ShowToast((String) msg.obj);
				PwdSettingActivity.this.setResult(2,
						new Intent().putExtra("back", "已设置>"));
				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit().putBoolean("pwdset", true).commit();
				PwdSettingActivity.this.finish();
				break;
			case ChangePwdFAIL_MSG:
				app.ShowToast(R.string.changePwdFail);
				PwdSettingActivity.this.finish();
				break;
			}
		}
	};

}
