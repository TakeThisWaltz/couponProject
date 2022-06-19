//package kr.azazel.barcode.reader;
//
///**
// * Created by ji on 2016. 10. 10..
// */
//
//import com.google.android.gms.vision.MultiProcessor;
//import com.google.android.gms.vision.Tracker;
//import com.google.android.gms.vision.barcode.Barcode;
//import com.google.mlkit.vision.barcode.common.Barcode;
//
//
//class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
//    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
//
//    BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay) {
//        mGraphicOverlay = barcodeGraphicOverlay;
//    }
//
//    @Override
//    public Tracker<Barcode> create(Barcode barcode) {
//        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
//        return new BarcodeGraphicTracker(mGraphicOverlay, graphic);
//    }
//
//}
