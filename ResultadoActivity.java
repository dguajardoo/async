package com.ibex.ventasmobile.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.ibex.ventasmobile.R;
import com.ibex.ventasmobile.elements.Constantes;
import com.ibex.ventasmobile.elements.Resultado;


import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ResultadoActivity extends AppCompatActivity {

    private TextView lblEtiqueta;
    private ListView lstOpciones;
    private Toolbar ctlLayout;
    private SwipeRefreshLayout refreshLayout;

    private String titulo = "";

    Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado);

        bundle = getIntent().getExtras();

        lstOpciones = (ListView)findViewById(R.id.LstOpciones);

        ctlLayout = (Toolbar)findViewById(R.id.appbar);
        titulo = "Ventas por Canal - "+ bundle.getString("TipoBusqueda");
        ctlLayout.setTitle(titulo);
        setSupportActionBar(ctlLayout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        ctlLayout.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);

        if (isOnline()) {
            resultado();
        } else {
            mensajeAlert("Mensaje", "NO HAY CONEXIÓN A INTERNET.");
            //Snackbar.make(view, "NO HAY CONEXIÓN A INTERNET.", Snackbar.LENGTH_SHORT).show();
        }

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isOnline()) {
                    resultadoRefresh();
                } else {
                    mensajeAlert("Mensaje", "NO HAY CONEXIÓN A INTERNET.");
                    refreshLayout.setRefreshing(false);
                }
            }
        });



    }

    private void resultado(){
        final Handler handler = new Handler();
        Runnable r = new Runnable(){
            public void run() {
                ProgressDialog progress = new ProgressDialog(ResultadoActivity.this);
                progress.setCancelable(false);
                progress.setCanceledOnTouchOutside(false);
                progress.setMessage("Cargando datos, por favor espere...");
                new Resultados(progress, ResultadoActivity.this).execute();
            }
        };
        handler.post(r);
    }

    private void resultadoRefresh(){
        final Handler handler = new Handler();
        Runnable r = new Runnable(){
            public void run() {
                new ResultadosRefresh(ResultadoActivity.this).execute();
                refreshLayout.setRefreshing(false);
            }
        };
        handler.post(r);
    }

    public void mensajeAlert(String titulo, String mensaje){

        new AlertDialog.Builder(ResultadoActivity.this)
                //.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Stop the activity
                        //ParametrosConsultaActivity.this.finish();
                    }

                })
                .show();
    }



    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }

        return false;
    }

    class AdaptadorTitulares extends ArrayAdapter<Resultado> {

        public AdaptadorTitulares(Context context, ArrayList<Resultado> datos) {
            super(context, R.layout.listitem_titular, datos);
        }

        TextView canal;
        TextView anno_anterior;
        TextView anno_actual;
        TextView neto_anterior;
        TextView neto_actual;
        TextView porcentaje;
        TextView ver_detalle;
        BarChart barChart;
        RelativeLayout content_ver_detalle;

        public String formateadorMontoConComa(String cadena){

            String decimal;
            String[] parts = cadena.split(",");
            if (parts.length > 1){
                decimal = "," + parts[1];
            } else {
                decimal = "";
            }
            Locale locale = new Locale("es","CL");
            NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
            Log.d("fgfhgdsa",cadena);
            String entero = nf.format(Integer.parseInt(parts[0]));
            return entero + decimal;
        }

        boolean isDouble(String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public String formateadorMontoConPunto(String cadena){

            String decimal;
            Log.d("cadena1", cadena);
            Locale locale = new Locale("es","CL");
            NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
            DecimalFormat df = (DecimalFormat)nf;
            df.applyPattern("###,###.##");

            if (isDouble(cadena)) {

                String[] parts = cadena.split(",");

                if (parts.length == 0) {
                    Log.d("parte1",parts.length+"");

                    String entero = df.format(Double.parseDouble(cadena));
                    return "$" + entero;
                } else {
                    Log.d("parte2", parts.length + "");
                    String entero = df.format(Double.parseDouble(parts[0]));
                    return "$" + entero;
                }

            } else {
                String entero = df.format(cadena);
                return "$" + entero;
            }

        }

        public String porcentaje (String porc) {
            double valor = Double.parseDouble(porc);

            if (valor > 0) {
                //porcentaje.setBackgroundColor(Color.parseColor("#76FF03"));
                //porcentaje.setBackgroundColor(Color.parseColor("#64DD17"));
                porcentaje.setTextColor(Color.parseColor("#FFFFFF"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#00A300"));
                gd.setCornerRadius(5);
                porcentaje.setBackgroundDrawable(gd);
                porcentaje.setPadding(10, 5, 10, 5);

                return "+" + valor + "%";
            } else if (valor == 0) {
                //porcentaje.setBackgroundColor(Color.parseColor("#76FF03"));
                //porcentaje.setTextColor(Color.parseColor("#64DD17"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FAFAFA"));
                gd.setCornerRadius(5);
                porcentaje.setBackgroundDrawable(gd);
                porcentaje.setTextColor(Color.parseColor("#000000"));
                return "" + valor + "%";
            }else {
                //porcentaje.setBackgroundColor(Color.parseColor("#F44336"));
                //porcentaje.setBackgroundColor(Color.parseColor("#F44336"));
                porcentaje.setTextColor(Color.parseColor("#FFFFFF"));
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#F44336"));
                gd.setCornerRadius(5);
                porcentaje.setBackgroundDrawable(gd);
                porcentaje.setPadding(10, 5, 10, 5);
                return "" + valor + "%";
            }
        }

        public void configDetalle(String orden, String colorDetalle, String colorbackground ){

            if (orden.equals("exportacion") || orden.equals("empresas relacionadas") || orden.equals("total")) {
                content_ver_detalle.setVisibility(View.GONE);
                ver_detalle.setVisibility(View.GONE);
                ver_detalle.setTextColor(Color.parseColor(colorDetalle));
            }

            if (orden.equals("minorista") || orden.equals("e-store") || orden.equals("mayorista")) {
                content_ver_detalle.setVisibility(View.VISIBLE);
                ver_detalle.setVisibility(View.VISIBLE);

                ver_detalle.setTextColor(Color.parseColor(colorDetalle));

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor(colorbackground));
                gd.setCornerRadius(5);
                ver_detalle.setBackgroundDrawable(gd);
            }



        }

        public View getView(int position, View convertView, ViewGroup parent) {

            Resultado re = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_titular, parent, false);
            }

            canal = (TextView)convertView.findViewById(R.id.lbl_canal);
            anno_anterior = (TextView)convertView.findViewById(R.id.lbl_año_anterior);
            anno_actual = (TextView)convertView.findViewById(R.id.lbl_año_actual);
            neto_anterior = (TextView)convertView.findViewById(R.id.lbl_neto_anterior);
            neto_actual = (TextView)convertView.findViewById(R.id.lbl_neto_actual);
            porcentaje = (TextView)convertView.findViewById(R.id.lbl_porcentaje);


            canal.setText(re.getCanal());
            anno_actual.setText(re.getAnno_actual());
            //Log.d("neto_anterior", re.getNeto_anterior());
            String aux = re.getNeto_actual();

            neto_actual.setText(formateadorMontoConPunto(re.getNeto_actual()));


            anno_anterior.setVisibility(View.GONE);
            neto_anterior.setVisibility(View.GONE);
            porcentaje.setVisibility(View.GONE);

            if (re.getCanal().toLowerCase().equals("minorista")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#848484");
            }

            if (re.getCanal().toLowerCase().equals("e-store")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#848484");
            }

            if (re.getCanal().toLowerCase().equals("mayorista")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#848484");

            }

            if (re.getCanal().toLowerCase().equals("exportacion")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#000000");
            }

            if (re.getCanal().toLowerCase().equals("empresas relacionadas")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#000000");
            }

            if (re.getCanal().toLowerCase().equals("total")) {
                content_ver_detalle = (RelativeLayout)convertView.findViewById(R.id.content_ver_detalle);
                ver_detalle = (TextView)convertView.findViewById(R.id.lbl_ver_detalle);
                configDetalle(re.getCanal().toLowerCase(), "#ffffff", "#000000");
            }

            BarChart barChart = (BarChart)convertView.findViewById(R.id.chart);

            if (!bundle.getString("TipoBusqueda").equals("Dia2")) {

                anno_anterior.setText(re.getAnno_anterior());
                neto_anterior.setText(formateadorMontoConPunto(re.getNeto_anterior()));
                porcentaje.setText(porcentaje(re.getPorcentaje()));

                anno_anterior.setVisibility(View.VISIBLE);
                neto_anterior.setVisibility(View.VISIBLE);
                porcentaje.setVisibility(View.VISIBLE);




                ArrayList<BarEntry> entries = new ArrayList<>();
                entries.add(new BarEntry(Float.parseFloat(re.getNeto_anterior()), 0));
                entries.add(new BarEntry(Float.parseFloat(re.getNeto_actual()), 1));


                BarDataSet dataset = new BarDataSet(entries, "# Años");
                dataset.setColors(ColorTemplate.JOYFUL_COLORS);

                ArrayList<String> labels = new ArrayList<String>();
                labels.add(re.getAnno_anterior());
                labels.add(re.getAnno_actual());

                BarData data = new BarData(labels, dataset);

                barChart.setData(data); // set the data and list of lables into chart
                barChart.setTouchEnabled(false);
                barChart.setPinchZoom(false);

                barChart.setDescription("");  // set the description
                barChart.setNoDataText("No existe información relacionada.");
                barChart.animateX(2000);
                barChart.animateY(2000);

                XAxis xAxis = barChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setTextSize(10f);
                xAxis.setDrawAxisLine(true);
                xAxis.setDrawGridLines(false);

                YAxis yAxis = barChart.getAxisRight();
                yAxis.setGranularityEnabled(false);
                yAxis.setDrawAxisLine(false);
                yAxis.setEnabled(false);


                YAxis yAxisL = barChart.getAxisLeft();
                yAxisL.setLabelCount(10, true);
                yAxisL.setAxisMinValue(0);


                barChart.setAutoScaleMinMaxEnabled(false);
                //barChart.setScaleMinima(1f,2f);

                Legend legend = barChart.getLegend();
                legend.setEnabled(false);



            } else {
                barChart.setVisibility(View.GONE);
                neto_actual.setPadding(10, 5, 10, 5);
            }


            return(convertView);
        }


    }

    public String calcularPorcentaje(String año_anterior, String año_actual){
        double anterior = Double.parseDouble(año_anterior);
        double actual = Double.parseDouble(año_actual);

        return ((100 * actual)/anterior)+"";
    }

    //Tarea Asíncrona para llamar al WS de consulta en segundo plano
    private class Resultados extends AsyncTask<String, Integer, Boolean> {
        //private Resultado[] listaResultado;
        private ArrayList<Resultado> listaResultado;
        ProgressDialog progress;
        ResultadoActivity act;

        public Resultados(ProgressDialog progress, ResultadoActivity act) {
            this.progress = progress;
            this.act = act;
        }

        public void onPreExecute() {

            progress.show();
            progress.setCanceledOnTouchOutside(false);
        }

        protected Boolean doInBackground(String... params){
            boolean resul = true;



            if (isOnline()) {

                if (bundle.getString("TipoBusqueda").equals("Dia")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BD, Constantes.METHOD_NAME_BD);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;
                    Log.d("ver", bundle.getString("Paises"));
                    Log.d("ver", strAnno_Ant+"");
                    Log.d("ver", bundle.getString("Año"));
                    Log.d("ver", bundle.getString("Mes"));
                    Log.d("ver", bundle.getString("Moneda"));
                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BD);
                    try {
                        //Thread.currentThread();
                        //Thread.sleep(5000);
                        transporte.call(Constantes.SOAP_ACTION_BD, envelope);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf-1", response.toString());
                        Log.d("asdf0", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() + " : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf1", datosxml.getProperty(0).toString());
                            Log.d("asdf2", datosxml.getProperty(1).toString());
                            Log.d("asdf3", datosxml.getProperty(2).toString());
                            Log.d("asdf4", datosxml.getProperty(3).toString());
                            Log.d("asdf5", datosxml.getProperty(4).toString());
                            Log.d("asdf6", datosxml.getProperty(5).toString());
                            Log.d("asdf7", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

                if (bundle.getString("TipoBusqueda").equals("Mes")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BM, Constantes.METHOD_NAME_BM);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BM);
                    try {
                        transporte.call(Constantes.SOAP_ACTION_BM, envelope);

                        //Thread.currentThread();
                        //Thread.sleep(5000);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf8", response.toString());
                        Log.d("asdf9", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() +" : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf10", datosxml.getProperty(0).toString());
                            Log.d("asdf11", datosxml.getProperty(1).toString());
                            Log.d("asdf12", datosxml.getProperty(2).toString());
                            Log.d("asdf13", datosxml.getProperty(3).toString());
                            Log.d("asdf14", datosxml.getProperty(4).toString());
                            Log.d("asdf15", datosxml.getProperty(5).toString());
                            Log.d("asdf16", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

                if (bundle.getString("TipoBusqueda").equals("Acumulado")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BA, Constantes.METHOD_NAME_BA);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BA);
                    try {
                        transporte.call(Constantes.SOAP_ACTION_BA, envelope);

                        //Thread.currentThread();
                        //Thread.sleep(5000);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf17", response.toString());
                        Log.d("asdf18", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() +" : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf19", datosxml.getProperty(0).toString());
                            Log.d("asdf20", datosxml.getProperty(1).toString());
                            Log.d("asdf21", datosxml.getProperty(2).toString());
                            Log.d("asdf22", datosxml.getProperty(3).toString());
                            Log.d("asdf23", datosxml.getProperty(4).toString());
                            Log.d("asdf24", datosxml.getProperty(5).toString());
                            Log.d("asdf25", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

            } else {
                mensajeAlertInicio("Mensaje", "NO HAY CONEXIÓN A INTERNET, VUELVE A CARGAR LA APLICACION.");

            }

            return resul;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            try {
                Log.d("qwq",""+result);
                //Log.d("qwq", "se cayó1");

                //Adaptador
                AdaptadorTitulares adaptador =
                        new AdaptadorTitulares(ResultadoActivity.this, listaResultado);

                lstOpciones.setAdapter(adaptador);
                lstOpciones.setEmptyView(findViewById(R.id.header_main_page_clist1));


                lstOpciones.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        int topRowVerticalPosition = (lstOpciones == null || lstOpciones.getChildCount() == 0) ?
                                0 : lstOpciones.getChildAt(0).getTop();
                        refreshLayout.setEnabled((topRowVerticalPosition >= 0));
                    }
                });
                //Eventos
                lstOpciones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> a, View v, int position, long id) {

                        Resultado item = ((Resultado)a.getItemAtPosition(position));

                        if (item.getOrden().equals("1")) {
                            Intent intent = new Intent(ResultadoActivity.this, ResultadoConsultaDetalleActivity.class);

                            int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                            Log.d("esdg1f", strAnno_Ant + "");

                            intent.putExtra("TipoBusqueda_detalle", bundle.getString("TipoBusqueda"));
                            intent.putExtra("Anno_Ant_detalle", strAnno_Ant+"");
                            intent.putExtra("Anno_detalle", bundle.getString("Año"));
                            intent.putExtra("Mes_detalle", bundle.getString("Mes"));
                            intent.putExtra("Canal_detalle", "MINORISTA");
                            intent.putExtra("Moneda_detalle", bundle.getString("Moneda"));
                            intent.putExtra("Pais_detalle", bundle.getString("Paises"));

                            //viewHolder.ver_detalle.setVisibility(View.VISIBLE);

                            startActivity(intent);
                        }

                        if (item.getOrden().equals("2")) {
                            Intent intent = new Intent(ResultadoActivity.this, ResultadoConsultaDetalleActivity.class);

                            int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                            Log.d("esdg1f", strAnno_Ant + "");

                            intent.putExtra("TipoBusqueda_detalle", bundle.getString("TipoBusqueda"));
                            intent.putExtra("Anno_Ant_detalle", strAnno_Ant+"");
                            intent.putExtra("Anno_detalle", bundle.getString("Año"));
                            intent.putExtra("Mes_detalle", bundle.getString("Mes"));
                            intent.putExtra("Canal_detalle", "E-STORE");
                            intent.putExtra("Moneda_detalle", bundle.getString("Moneda"));
                            intent.putExtra("Pais_detalle", bundle.getString("Paises"));

                            //viewHolder.ver_detalle.setVisibility(View.VISIBLE);

                            startActivity(intent);
                        }

                        if (item.getOrden().equals("3")) {
                            Intent intent = new Intent(ResultadoActivity.this, ResultadoConsultaDetalleActivity.class);

                            int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                            intent.putExtra("TipoBusqueda_detalle", bundle.getString("TipoBusqueda"));
                            intent.putExtra("Anno_Ant_detalle", strAnno_Ant+"");
                            intent.putExtra("Anno_detalle", bundle.getString("Año"));
                            intent.putExtra("Mes_detalle", bundle.getString("Mes"));
                            intent.putExtra("Canal_detalle", "MAYORISTA");
                            intent.putExtra("Moneda_detalle", bundle.getString("Moneda"));
                            intent.putExtra("Pais_detalle", bundle.getString("Paises"));

                            startActivity(intent);
                        }
                    }
                });

                progress.dismiss();
            } catch (Exception e) {
                Toast.makeText(ResultadoActivity.this, "Vuelva a intentar", Toast.LENGTH_SHORT).show();
                finish();
            }



        }
    }

    public void mensajeAlertInicio(String titulo, String mensaje){

        new android.support.v7.app.AlertDialog.Builder(ResultadoActivity.this)
                //.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .show();
    }

    //Tarea Asíncrona para llamar al WS de consulta en segundo plano
    private class ResultadosRefresh extends AsyncTask<String, Integer, Boolean> {
        //private Resultado[] listaResultado;
        private ArrayList<Resultado> listaResultado;
        ProgressDialog progress;
        ResultadoActivity act;

        public ResultadosRefresh(ResultadoActivity act) {
            this.act = act;
        }

        public void onPreExecute() {

            //progress.show();
            //progress.setCanceledOnTouchOutside(false);
        }

        protected Boolean doInBackground(String... params){
            boolean resul = true;



            if (isOnline()) {

                if (bundle.getString("TipoBusqueda").equals("Dia")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BD, Constantes.METHOD_NAME_BD);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BD);
                    try {

                        transporte.call(Constantes.SOAP_ACTION_BD, envelope);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf26", response.toString());
                        Log.d("asdf27", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() + " : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf28", datosxml.getProperty(0).toString());
                            Log.d("asdf29", datosxml.getProperty(1).toString());
                            Log.d("asdf30", datosxml.getProperty(2).toString());
                            Log.d("asdf31", datosxml.getProperty(3).toString());
                            Log.d("asdf32", datosxml.getProperty(4).toString());
                            Log.d("asdf33", datosxml.getProperty(5).toString());
                            Log.d("asdf34", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

                if (bundle.getString("TipoBusqueda").equals("Mes")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BM, Constantes.METHOD_NAME_BM);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BM);
                    try {
                        transporte.call(Constantes.SOAP_ACTION_BM, envelope);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf35", response.toString());
                        Log.d("asdf36", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() +" : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf37", datosxml.getProperty(0).toString());
                            Log.d("asdf38", datosxml.getProperty(1).toString());
                            Log.d("asdf39", datosxml.getProperty(2).toString());
                            Log.d("asdf40", datosxml.getProperty(3).toString());
                            Log.d("asdf41", datosxml.getProperty(4).toString());
                            Log.d("asdf42", datosxml.getProperty(5).toString());
                            Log.d("asdf43", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

                if (bundle.getString("TipoBusqueda").equals("Acumulado")){
                    SoapObject request = new SoapObject(Constantes.NAMESPACE_BA, Constantes.METHOD_NAME_BA);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    // <strPais>0CH0,0AR0</strPais>
                    int strAnno_Ant = Integer.parseInt(bundle.getString("Año"))-1;

                    request.addProperty("strPais", bundle.getString("Paises"));
                    request.addProperty("strAnno_Ant", strAnno_Ant);
                    request.addProperty("strAnno", bundle.getString("Año"));
                    request.addProperty("strMes", bundle.getString("Mes"));
                    request.addProperty("strMoneda", bundle.getString("Moneda"));

                    envelope.dotNet = true;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE transporte = new HttpTransportSE(Constantes.URL_BA);
                    try {
                        transporte.call(Constantes.SOAP_ACTION_BA, envelope);

                        SoapObject response = (SoapObject)envelope.getResponse();
                        SoapObject linVentas = (SoapObject)response.getProperty("linVentas");
                        //SoapObject linVenta = (SoapObject)linVentas.getProperty("linVenta");
                        Log.d("asdf44", response.toString());
                        Log.d("asdf45", linVentas.toString());
                        //Log.d("asdf", linVenta.toString());



                        listaResultado = new ArrayList<>();
                        int fila = 0;
                        for (int i = 0; i < linVentas.getPropertyCount(); i++) {
                            SoapObject datosxml = (SoapObject)linVentas.getProperty(i);

                            //Log.d("ass<zxdf", linVentas.toString());
                            Resultado res = new Resultado();
                            res.setCanal(datosxml.getProperty(0).toString());
                            res.setOrden(datosxml.getProperty(1).toString());
                            res.setAnno_anterior("Año " + datosxml.getProperty(2).toString() + " : ");
                            res.setAnno_actual("Año " + datosxml.getProperty(3).toString() +" : ");
                            res.setNeto_anterior(datosxml.getProperty(4).toString());
                            res.setNeto_actual(datosxml.getProperty(5).toString());
                            res.setPorcentaje(datosxml.getProperty(6).toString());

                            //Log.d("asdf", datosxml.getProperty(0).toString());
                            Log.d("asdf46", datosxml.getProperty(0).toString());
                            Log.d("asdf47", datosxml.getProperty(1).toString());
                            Log.d("asdf48", datosxml.getProperty(2).toString());
                            Log.d("asdf49", datosxml.getProperty(3).toString());
                            Log.d("asdf50", datosxml.getProperty(4).toString());
                            Log.d("asdf61", datosxml.getProperty(5).toString());
                            Log.d("asdf62", datosxml.getProperty(6).toString());


                            listaResultado.add(res);

                        }

                        Log.d("lista", listaResultado.toString());



                    } catch (Exception e) {
                        Log.d("qwq","se cayó");
                        Log.d("qwq",e.getMessage());

                        resul = false;
                    }
                }

            } else {
                mensajeAlertInicio("Mensaje", "NO HAY CONEXIÓN A INTERNET, VUELVE A CARGAR LA APLICACION.");
            }

            return resul;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            try {
                Log.d("qwq",""+result);
                //Log.d("qwq", "se cayó1");




                //Adaptador
                AdaptadorTitulares adaptador =
                        new AdaptadorTitulares(ResultadoActivity.this, listaResultado);

                lstOpciones.setAdapter(adaptador);
                lstOpciones.setEmptyView(findViewById(R.id.header_main_page_clist1));

                Toast.makeText(ResultadoActivity.this, "Información actualizada exitosamente.", Toast.LENGTH_SHORT).show();

                lstOpciones.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        int topRowVerticalPosition = (lstOpciones == null || lstOpciones.getChildCount() == 0) ?
                                0 : lstOpciones.getChildAt(0).getTop();
                        refreshLayout.setEnabled((topRowVerticalPosition >= 0));
                    }
                });

                //Eventos
                lstOpciones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> a, View v, int position, long id) {

                        Resultado item = ((Resultado) a.getItemAtPosition(position));

                        if (item.getOrden().equals("1")) {
                            Intent intent = new Intent(ResultadoActivity.this, ResultadoConsultaDetalleActivity.class);

                            int strAnno_Ant = Integer.parseInt(bundle.getString("Año")) - 1;

                            Log.d("esdg1f", strAnno_Ant + "");

                            intent.putExtra("TipoBusqueda_detalle", bundle.getString("TipoBusqueda"));
                            intent.putExtra("Anno_Ant_detalle", strAnno_Ant + "");
                            intent.putExtra("Anno_detalle", bundle.getString("Año"));
                            intent.putExtra("Mes_detalle", bundle.getString("Mes"));
                            intent.putExtra("Canal_detalle", "MINORISTA");
                            intent.putExtra("Moneda_detalle", bundle.getString("Moneda"));
                            intent.putExtra("Pais_detalle", bundle.getString("Paises"));

                            //viewHolder.ver_detalle.setVisibility(View.VISIBLE);

                            startActivity(intent);
                        }

                        if (item.getOrden().equals("3")) {
                            Intent intent = new Intent(ResultadoActivity.this, ResultadoConsultaDetalleActivity.class);

                            int strAnno_Ant = Integer.parseInt(bundle.getString("Año")) - 1;

                            intent.putExtra("TipoBusqueda_detalle", bundle.getString("TipoBusqueda"));
                            intent.putExtra("Anno_Ant_detalle", strAnno_Ant + "");
                            intent.putExtra("Anno_detalle", bundle.getString("Año"));
                            intent.putExtra("Mes_detalle", bundle.getString("Mes"));
                            intent.putExtra("Canal_detalle", "MAYORISTA");
                            intent.putExtra("Moneda_detalle", bundle.getString("Moneda"));
                            intent.putExtra("Pais_detalle", bundle.getString("Paises"));

                            startActivity(intent);
                        }
                    }
                });
            } catch (Exception e) {
                Toast.makeText(ResultadoActivity.this, "Vuelva a intentar", Toast.LENGTH_SHORT).show();
                finish();
            }



        }
    }

    public class LargeValueFormatter implements ValueFormatter, YAxisValueFormatter {

        private String[] SUFFIX = new String[]{
                "", "k", "m", "b", "t"
        };
        private static final int MAX_LENGTH = 4;
        private DecimalFormat mFormat;
        private String mText = "";

        public LargeValueFormatter() {
            mFormat = new DecimalFormat("###E0");
        }

        /**
         * Creates a formatter that appends a specified text to the result string
         *
         * @param appendix a text that will be appended
         */
        public LargeValueFormatter(String appendix) {
            this();
            mText = appendix;
        }

        // ValueFormatter
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return makePretty(value) + mText;
        }

        // YAxisValueFormatter
        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return makePretty(value) + mText;
        }

        /**
         * Set an appendix text to be added at the end of the formatted value.
         *
         * @param appendix
         */
        public void setAppendix(String appendix) {
            this.mText = appendix;
        }

        /**
         * Set custom suffix to be appended after the values.
         * Default suffix: ["", "k", "m", "b", "t"]
         *
         * @param suff new suffix
         */
        public void setSuffix(String[] suff) {
            if (suff.length == 5) {
                SUFFIX = suff;
            }
        }

        /**
         * Formats each number properly. Special thanks to Roman Gromov
         * (https://github.com/romangromov) for this piece of code.
         */
        private String makePretty(double number) {

            String r = mFormat.format(number);

            r = r.replaceAll("E[0-9]", SUFFIX[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);

            while (r.length() > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]")) {
                r = r.substring(0, r.length() - 2) + r.substring(r.length() - 1);
            }

            return r;
        }
    }

}
