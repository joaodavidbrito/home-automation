package ha.joaobrito.pt.ha;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.helper.SingletonHelper;

public class PrintActivity extends AppCompatActivity {


    private static final String IMAGE_ENDPOINT = "/images/";
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);

        new NetworkTask().execute();
    }

    private final class CustomAdapter implements ListAdapter {
        private final String TAG = CustomAdapter.class.getName();
        private final List<ImagesDto> myList;
        private final Context context;

        public CustomAdapter(Context context, List<ImagesDto> myList) {
            this.myList = myList;
            this.context = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ImagesDto dto = myList.get(position);
            if (view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                view = layoutInflater.inflate(R.layout.activity_listview, null);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // todo pass view to other activity
                        Log.d(TAG, "click");
                    }
                });
                TextView tittle = view.findViewById(R.id.listview_item_title);
                tittle.setText(dto.getObjectDesc());

                TextView id = view.findViewById(R.id.listview_item_id);
                id.setText(dto.getId());

                TextView action = view.findViewById(R.id.listview_item_action);
                action.setText(dto.getAction());

                ImageView imag = view.findViewById(R.id.listview_image);
                byte[] img = dto.getEncodedImage();
                Bitmap bMap = BitmapFactory.decodeByteArray(img, 0, img.length);
                imag.setImageBitmap(bMap);
            }
            return view;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return myList.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private class PrintTask extends AsyncTask<Void, String, List<ImagesDto>> {

        private static final String OBJECTS_ENDPOINT = "/images/objects";
        private final String TAG = PrintTask.class.getName();

        @Override
        protected List<ImagesDto> doInBackground(Void... data) {

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 4000);
            HttpConnectionParams.setSoTimeout(params, 500);
            HttpClient httpclient = new DefaultHttpClient(params);

            HttpResponse response;
            ObjectInputStream ois = null;
            try {
                String serverHost = SingletonHelper.getInstance(PrintActivity.this).getOpencvHost();
                HttpGet httpget = new HttpGet("http://" + serverHost + OBJECTS_ENDPOINT);
                response = httpclient.execute(httpget);

                ois = new ObjectInputStream(response.getEntity().getContent());
                return (List<ImagesDto>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Log.e(null, e.getMessage());
            } finally {
                try {
                    if(ois != null){
                        ois.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<ImagesDto> images) {
            if(images == null){
                Toast.makeText(PrintActivity.this, "The server can not be reached. Please retry...", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            CustomAdapter customAdapter = new CustomAdapter(PrintActivity.this, images);
            PrintActivity.this.list = findViewById(R.id.list_view);
            PrintActivity.this.list.setAdapter(customAdapter);
        }
    }

    private final class NetworkTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... v) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 200);
            HttpConnectionParams.setSoTimeout(params, 200);
            HttpClient httpclient = new DefaultHttpClient(params);

            HttpOptions options = new HttpOptions("http://" + SingletonHelper.getInstance(PrintActivity.this).getOpencvHost() + SingletonHelper.HELLO_ENDPOINT);
            try {
                httpclient.execute(options);
            } catch (Exception e) {
                return null;
            }
            return "hello";
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            if (res == null) {
                Toast.makeText(PrintActivity.this, "The server can not be reached. Please retry...", Toast.LENGTH_SHORT).show();
                PrintActivity.this.finish();
            }else{
                new PrintTask().execute();
            }
        }
    }


}
