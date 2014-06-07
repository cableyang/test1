package iflab.test;

import iflab.model.ECG;
import iflab.myinterface.EcgDAO;
import iflab.myinterface.ElderDAO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


public class firstActivity extends Activity  
{
	private Timer bttimer = new Timer();
	private TimerTask bttask;
	public int timercount;
	boolean httpstart; //��ʼHTTP����
	private Timer httptiTimer = new Timer();
	private TimerTask httptTask;
	private boolean bttimeflag=true;

    private static Handler drhandler;
 
    Store2Sqlite store2Sqlite;
	
	/*
	 * �����ַ������
	 */
	public static final int RATE250 = 250;
	public static final int RATE500 = 500;
	public static final int RATE1000 = 1000;
	
	/*
	 * ������Ϣ����Σ�����MU_UUIDΪSPP�����ID
	 */
	private final static int REQUEST_CONNECT_DEVICE = 1;    //�궨���ѯ�豸��	
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP����UUID��	
    private InputStream blueStream;    //������������������������
    private static String readMessage="";
	private static String firstmessage;//��ŵ�һ����Ϣ
	private HttpBindService mBindService;
	
	boolean mIsBound;
	
	
	BluetoothDevice _device = null;     //�����豸
    BluetoothSocket _socket = null;      //����ͨ��socket
    boolean _discoveryFinished = false;    
    boolean bRun = true;
    boolean bThread = false;
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //��ȡ�����������������������豸
	
    //sensor demo
    private SensorManager mSensorManager;
    private Sensor mLight;
	/*
	 * ��ʾ���涨��� ����
	 */
	public GraphicsData graphicsECGData;
	public GraphicsData accdataX,accdataY,accdataZ;
	
	private MyGraphics myGraphics1=null;			//�����Զ���View����
//	private MyGraphics myGraphics2=null;
	private PlotOfAcc plotOfAcc=null;
	ElderDAO elderDAO;  //�����������ݶ���
	EcgDAO ecgDAO;
	HttpECGservice httpECGservice; //����httpecgԶ�̷�����
	/*
	 * ��ť����
	 */
	Button blueStartButton;  //��������
	Button bindButton;
	Button btnperson; //��������Ϣbutton
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {	//����onCreate����
		final int packetnum = 14;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	    LinearLayout layout_ecg = (LinearLayout)findViewById(R.id.ECG_SINGAL);
	    LinearLayout layout_pluse = (LinearLayout)findViewById(R.id.Pluse);
	    
	    
	    //���ĵ��ź���ʾ���ֳ�ʼ��
	    graphicsECGData = new GraphicsData(RATE500); //�ֱ��ECG��PLUSE����Ƶ���趨
	    this.myGraphics1=new MyGraphics(this, RATE500,  graphicsECGData);		//�����Զ���View����
	    layout_ecg.addView(myGraphics1, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));  
	    double []p= new double [500];
	    for (int i = 0; i < RATE500; i++)
		{
		
		 graphicsECGData.adddata(300-0);
		}
	    
	    //�Լ��ٶ��ź���ʾ���ֽ��г�ʼ��
	    accdataX = new GraphicsData(RATE500);
	    accdataY = new GraphicsData(RATE500);
	    accdataZ = new GraphicsData(RATE500);
	    this.plotOfAcc=new PlotOfAcc(this, RATE500, accdataX,accdataY,accdataZ);		//�����Զ���View����
	    layout_pluse.addView(plotOfAcc, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        for (int i = 0; i < RATE500; i++)
		{ 
		 accdataX.adddata(0);
		 accdataY.adddata(0);
		 accdataZ.adddata(0);
		}
     
        //UI���ֳ�ʼ��
	    bindButton =(Button)findViewById(R.id.Bind);
	    bindButton.setOnClickListener(listener);
	  
	    blueStartButton = (Button)findViewById(R.id.bluestart);
	    blueStartButton.setOnClickListener(listener);
        
	    btnperson=(Button)findViewById(R.id.person);
	    btnperson.setOnClickListener(listener);
	    /*
	     * Ӳ����Դ��ʼ�������豸�����ٶȼƵ��Դ�������
	     * @with register of acc
	     */
	          BluetoothCheck(); //check that device
	          
	          mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    	   mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		      Toast.makeText(getApplicationContext(), mLight.getName()+mLight.getVersion()+mLight.getVendor(), Toast.LENGTH_LONG).show();
		      SensorEventListener aListener = new SensorEventListener()
			{
				//���ٶ�ֵ�仯ʱ����ת��
				@Override
				public void onSensorChanged(SensorEvent event)
				{
					// TODO Auto-generated method stub
					String str1,str2,str3;
					accdataX.adddata(event.values[0]);
					accdataY.adddata(event.values[1]);
					accdataZ.adddata(event.values[2]);
				}
				
				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy)
				{
					// TODO Auto-generated method stub
					
				}
			};
		   
	      mSensorManager.registerListener(aListener, mLight,100);
	    
	     /*
	      * ****----ECGdata------->>>>SQLITE<<<<<<<<<<<<------
	      * ****----elderDAO------>>>>SQLITE<<<<<<<<<<<<------
	      */
	     ecgDAO = new EcgDAO(getBaseContext()); 
	     elderDAO= new ElderDAO(getBaseContext());
	     ecgDAO.createtable();
	     elderDAO.creattable();
	     store2Sqlite=new Store2Sqlite(ecgDAO,graphicsECGData);
	     store2Sqlite.StartStroing();
	     
	     /*
	      * ****----ECGdata------->>>>PHP using <<<<<<<<<<<------ 
	      * @httppost method and timertask 50bits and 10HZ
	      * @under json data type<
	      */	
	     ECG ecg=new ECG(1, "�", null, null, 0, 0);
	     httpECGservice= new HttpECGservice(ecg, graphicsECGData);
	         
	     httpECGservice.StartThread();
       
	    drhandler = new Handler() 
		{
               
			@Override
        	public void handleMessage(Message msg) 
        	{
        		 switch (msg.what) 
        		 {   
	        	 	case 1: 	
	        	 		Log.i("msg.what", "msg what is "+msg.what);
	        	 		// httpECGservice.httpStart=true;  //����ʼ��������
	        	 		 httpECGservice.StartSending(graphicsECGData);
	        		 break;
	        	 	case 2:  //��ʱ���ж� 	    

	        	    int num;	
					try
					{
				    byte []bytes=new byte[1024];
					Log.i("BUFFER IS", "firstmessage is "+firstmessage);	
					num = blueStream.read(bytes);
					if (httpstart)
					{
						httpECGservice.StartSending(graphicsECGData);
					}
					
					readMessage = new String(bytes, 0, num);
					Log.i("deal with things", "begin");
					graphicsECGData.dealwithstring(readMessage);
					Log.i("deal with things", "stop");
					//��ʼ��������
                  	
					Log.i("BUFFER IS", "readMessage is "+readMessage);
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 
		        	break;	 
        		 }
        		 
        		super.handleMessage(msg);
        	}
        };     
	  }  

	   void doBindService() 
	   { 
		   bindService(new Intent(firstActivity.this,HttpBindService.class), mConnection, Context.BIND_AUTO_CREATE);
	       mIsBound = true;
	       Log.i("log_tag", "is do binding service	..");
	    	}

	    	void doUnbindService() {
	    	    if (mIsBound) {
	    	        // Detach our existing connection.
	    	        unbindService(mConnection);
	    	        mIsBound = false;
	    	    }
	    	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
	 

	public static String charToHexString(byte strPart) 
    {
        
    	  String hex = Integer.toHexString(strPart & 0x00FF);
    	  if (hex.length() == 1) 
    	  {        
    		  hex = '0' + hex;      
    	  } 
    	  hex = "0x" + hex;
        return hex;
    }
 
 ServiceConnection mConnection = new ServiceConnection() 
 {  
			@Override
			public void onServiceConnected(ComponentName name,IBinder service)
			{
				 
 	    	mBindService = ((HttpBindService.LocalBinder)service).getService();
 	        // Tell the user about this for our demo.
 	    	mIsBound=true;
 	    	Log.i("log_tag", " on service connected..");  	
			}
			@Override
			public void onServiceDisconnected(ComponentName name)
			{
		   
			mIsBound=false;
 	        mBindService = null;   
			}
 	};
 
	/*
	 * check is bluetooth is available
	 * create a thread to always open the bluetooth
	 * and run it
	 */
	private void BluetoothCheck()
	{
		
	    if (_bluetooth == null)
        {
        	Toast.makeText(this, "�޷����ֻ���������ȷ���ֻ��Ƿ����������ܣ�", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
	    
	    new Thread()
	       {
	    	   public void run()
	    	   {
	    		   if(_bluetooth.isEnabled()==false)
	    		   {
	        		_bluetooth.enable();
	    		   }
	    	   }   	   
	       }.start();
	}
	
    /*
     * -----------------UI interface----------------------------------
     * ----------------deal with all the main button message----------------
     * ----------------�������а�ť��Ϣ--------------------------------
     */
	public OnClickListener listener = new OnClickListener()
	{
		
		@Override
		public void onClick(View v)
		{
			Button btn = (Button)v;
			switch (btn.getId())
			{
			case R.id.bluestart:
				StartBluetooth();  //������������
				break;
            
			case R.id.person:
				
				Intent intent=new Intent();
				intent.setClass(firstActivity.this, PatientID.class);
				intent.putExtra("str", "Intent Demo");
				//startActivity(intent);
				startActivityForResult(intent, 1);
				break;
				
			case R.id.Bind:
				if (mIsBound) {
		            // Call a method from the LocalService.
		            // However, if this call were something that might hang, then this request should
		            // occur in a separate thread to avoid slowing down the activity performance.
		            int num = mBindService.getRandomNumber();
			 
		           Log.i("log_tag", "number is"+num);
		        }
				break;

			default:
				break;
			}
		}
	};
	
	/*
	 * function check the bluetooth module 
	 * ����������Դ��ڣ�������ϢΪ�����ͻ��˿ڵĵ�ַ
	 */
	public void StartBluetooth()
    { 
		Toast.makeText(this, " ��������...", Toast.LENGTH_LONG).show();
		Intent serverIntent = new Intent(this, DeviceListActivity.class); //��ת��������
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //���÷��غ궨��
		
    	
		if(_bluetooth.isEnabled()==false)
    	{  //����������񲻿�������ʾ
    		Toast.makeText(this, " ��������...", Toast.LENGTH_LONG).show();
    		return;
    	}
 	
 	   if (bttimeflag==false)
    	{
    		if (bttask!=null)
    		{
    			bttask.cancel();
    		}
    		bttimeflag=true;
    	}
 	
    	if(_socket==null)
    	{
    		Intent serverIntent1 = new Intent(this, DeviceListActivity.class); //��ת��������
    		startActivityForResult(serverIntent1, REQUEST_CONNECT_DEVICE);  //���÷��غ궨��
    	}
    	else
    	{
    		 //�ر�����socket
    	    try
    	    {    	    	
    	    	blueStream.close();
    	    	_socket.close();
    	    	_socket = null;
    	    	bRun = false;
    	    	//ConB.setText("����");
    	    }
    	    catch(IOException e)
    	    {
    	    	
    	    }   
    	}
    }	
		
	 //���ջ�������ӦstartActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode){
    	case REQUEST_CONNECT_DEVICE:     //���ӽ������DeviceListActivity���÷���
    		// ��Ӧ���ؽ��
            if (resultCode == Activity.RESULT_OK) {   //���ӳɹ�����DeviceListActivity���÷���
                // MAC��ַ����DeviceListActivity���÷���
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // �õ������豸���      
                _device = _bluetooth.getRemoteDevice(address);
 
                // �÷���ŵõ�socket
                try{
                	_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch(IOException e){
                	Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
                }
                try
				{	
					_socket.connect();
					Toast.makeText(this, "����"+_device.getName()+"�ɹ���", Toast.LENGTH_SHORT).show();
				} catch (IOException e)
				{
					
            		try
					{
            		Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();
					_socket.close();
					_socket = null;
					} catch (IOException e1)
					{
						// TODO Auto-generated catch block
						Toast.makeText(this, "����ʧ�ܣ�", Toast.LENGTH_SHORT).show();	
					}            		
					// TODO Auto-generated catch block
					return;
				}
       
                //�򿪽����߳�
                try{
            		blueStream = _socket.getInputStream();   //�õ���������������
            		//blueoutOutputStream=_socket.getOutputStream();//�õ������������
            		Toast.makeText(this, "���������ɹ�", Toast.LENGTH_SHORT).show();
            		}catch(IOException e){
            			Toast.makeText(this, "��������ʧ�ܣ�", Toast.LENGTH_SHORT).show();
            			return;
            		}
            		
            		
            		if(bThread==false){
			
            //================���������Ӻ���г�ʼ��=================//
            //=====================================================//
            			timercount=0;
            			httpstart=false;
            			
            			bttask = new TimerTask() 
	        	        {
            				 
	        	        	@Override
	        	        	public void run() 
	        	        	{
	        	        		//����Ϣ���ڸ���ui��Ϣ
	        	                timercount++;
	        	                if (timercount==14)
								{
									timercount=0;
									httpstart=true; 
									
								}
	        	        		Message message = new Message();	 
	        	        		message.what = 2;
	        	        	    drhandler.sendMessage(message);
	        	        	   
	        	        	}
	        	        };
	        	        bttimeflag=false; 
	                    bttimer.schedule(bttask, 500, 35*2);
	                    
	          //-------------����HTTP������Ϣ����-----------------//
	          //---------------------------------------------//
	                 /*   httptTask = new TimerTask()
						{
 		                  @Override
							public void run()
							{
								// TODO Auto-generated method stub	  
 		                	  //���ڴ��������ϴ�
 		                		Message message = new Message();	 
	        	        		message.what = 1;
	        	        	    drhandler.sendMessage(message);
							}
						};
					   httptiTimer.schedule(httptTask, 500, 1000);	
                          */
            		}else{
            			bRun = true;
            		}  
            }
    		break;
    	default:break;
    	}  
    }	

  //���������߳�����ȡ��������
    Thread ReadThread=new Thread(){
     
    	 
   };


}


