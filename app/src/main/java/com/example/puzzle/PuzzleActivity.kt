package com.example.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class PuzzleActivity : AppCompatActivity() {

    var pieces :ArrayList<PuzzlePiece>? = null
    var mCurrentPhotoPath :String? = null
    var mCurrentPhotoUri :String? = null


//12345


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)
        val layout = findViewById<RelativeLayout>(R.id.layout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        //1234567

        val intent = intent
        val assetName = intent.getStringExtra("assetName")
        mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath")
        mCurrentPhotoUri = intent.getStringExtra("mCurrentPhotoUri")


        imageView.post {
            if (assetName != null){
                setPicFromAsset(assetName,imageView)
            }
            else if (mCurrentPhotoPath != null){
                setPicFromPhotoPath(mCurrentPhotoPath!!,imageView)
            }
            else if (mCurrentPhotoUri != null){
                imageView.setImageURI(Uri.parse(mCurrentPhotoUri))
            }
            pieces = splitImage()
            val touchListner = TouchListner(this@PuzzleActivity)
            Collections.shuffle(pieces)
            for (piece in pieces!!){
                piece.setOnTouchListener(touchListner)
                layout.addView(piece)

                val lParams = piece.layoutParams as RelativeLayout.LayoutParams
                lParams.leftMargin = kotlin.random.Random.nextInt(
                    layout.width - piece.pieceWidth
                )

                lParams.topMargin = layout.height - piece.pieceHeight
                piece.layoutParams = lParams

            }
        }

    }



    private fun setPicFromPhotoPath(mCurrentPhotoPath: String, imageView: ImageView?) {
        val targetW = imageView!!.width
        val targetH = imageView!!.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath,bmOptions)

        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        val scaleFactor = Math.min(
            photoW/targetW,photoH/targetH
        )

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,bmOptions)

        var rotatedBitmap = bitmap

        try {
            val ei = ExifInterface(mCurrentPhotoPath)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when(orientation){
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    rotatedBitmap = rotateImage(bitmap,90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    rotatedBitmap = rotateImage(bitmap,180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    rotatedBitmap = rotateImage(bitmap,270f)
                }

            }




        }catch (e:IOException){
            e.printStackTrace()
            Toast.makeText(this@PuzzleActivity,e.localizedMessage, Toast.LENGTH_SHORT).show()
        }

        imageView.setImageBitmap(rotatedBitmap)
    }

    private fun setPicFromAsset(assetName: String, imageView: ImageView?) {

        val targetW = imageView!!.width
        val targetH = imageView.height
        val am = assets

        try {

            val inputStream = am.open("img/$assetName")
            val bmOPtion = BitmapFactory.Options()
            BitmapFactory.decodeStream(
                inputStream, Rect(-1, -1, -1, -1), bmOPtion
            )

            val photoW = bmOPtion.outWidth
            val photoH = bmOPtion.outHeight

            val scalFctor = Math.min(
                photoW/targetW,photoH/targetH
            )
            bmOPtion.inJustDecodeBounds = false
            bmOPtion.inSampleSize = scalFctor
            bmOPtion.inPurgeable = true

            val bitmap = BitmapFactory.decodeStream(inputStream, Rect(-1, -1, -1, -1), bmOPtion)
            imageView.setImageBitmap(bitmap)





        }catch (e:IOException){
            e.printStackTrace()
            Toast.makeText(this@PuzzleActivity, "", Toast.LENGTH_SHORT).show()

        }

    }
    private fun splitImage(): ArrayList<PuzzlePiece> {
        val piecesNumber = 12
        val rows = 4
        var cols = 3
        val imageView = findViewById<ImageView>(R.id.imageView)
        val pieces = kotlin.collections.ArrayList<PuzzlePiece>(piecesNumber)
        val drawable = imageView.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val dimensions = getBitmapPositionInsideImageView(imageView)

        val scaledBitmapLeft = dimensions[0]
        val scaledBitmapTop = dimensions[1]
        val scaledBitmapWidth = dimensions[2]
        val scaledBitmapHeight = dimensions[3]
        val croppedImageWidth = scaledBitmapWidth - 2 * Math.abs(scaledBitmapLeft)
        val croppedImageHeight = scaledBitmapHeight - 2 * Math.abs(scaledBitmapTop)

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,scaledBitmapWidth,scaledBitmapHeight,true
        )
        val croppedBitmap = Bitmap.createBitmap( scaledBitmap,Math.abs(scaledBitmapLeft),Math.abs(scaledBitmapTop),croppedImageWidth,croppedImageHeight )

        val pieceWidth = croppedImageWidth/cols
        val pieceHeight = croppedImageHeight/rows

        var yCoord = 0
        for (row in 0 until rows)
        {
            var xCoord = 0
            for (col in 0 until cols){
                var offsetX = 0
                var offsetY = 0

                if (col > 0){
                    offsetX = pieceWidth / 3

                }
                if (row > 0 ){
                    offsetY = pieceHeight/3
                }

                val pieceBitmap = Bitmap.createBitmap(
                    croppedBitmap,xCoord - offsetX,yCoord - offsetY,
                    pieceWidth + offsetX, pieceHeight + offsetY
                )

                val piece = PuzzlePiece(applicationContext)
                piece.setImageBitmap(pieceBitmap)
                piece.xCoord = xCoord - offsetX + imageView.left
                piece.yCoord = yCoord - offsetY + imageView.top

                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY

                val puzzlePiece = Bitmap.createBitmap(
                    pieceWidth + offsetX, pieceHeight + offsetY,
                    Bitmap.Config.ARGB_8888
                )
                val bumpSize = pieceHeight/4
                val canvas = Canvas(puzzlePiece)
                val path = Path()
                path.moveTo(offsetX.toFloat(),offsetY.toFloat())

                if (row == 0){
                    path.lineTo(
                        pieceBitmap.width.toFloat(),
                        offsetY.toFloat()
                    )
                }
                else{
                    path.lineTo(
                        (offsetX+(pieceBitmap.width - offsetX)/3).toFloat(),
                        offsetY.toFloat()
                    )
                    path.cubicTo(
                        (offsetX + (pieceBitmap.width - offsetX).toFloat()),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX) / 6 *5).toFloat(),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX)/ 3* 2).toFloat(),
                        offsetY.toFloat()

                    )

                    path.lineTo(pieceBitmap.width.toFloat(),offsetY.toFloat())


                    if (row == 0) {

                    }
                    else{

                    }
                    if (col == cols - 1){
                        path.lineTo(
                            pieceBitmap.width.toFloat(),
                            pieceBitmap.height.toFloat()
                        )
                    }
                    else{
                        path.lineTo(
                            pieceBitmap.width.toFloat(),
                            (offsetY + (pieceBitmap.height - offsetY)/ 3 ).toFloat()

                        )
                        path.cubicTo(
                            (pieceBitmap.width - bumpSize).toFloat(),
                            (offsetY + (pieceBitmap.height - offsetY)/ 6 ).toFloat(),
                            (pieceBitmap.width - bumpSize).toFloat(),
                            (offsetY + (pieceBitmap.height - offsetY)/ 6 * 5 ).toFloat(),
                            pieceBitmap.width.toFloat(),
                            (offsetY + (pieceBitmap.height - offsetY)/ 3 * 2 ).toFloat(),






                        )

                        path.lineTo(
                            pieceBitmap.width.toFloat(),
                            pieceBitmap.height.toFloat()
                        )

                    }

                    if (row == -1){

                        path.lineTo(
                            offsetX.toFloat(),pieceBitmap.height.toFloat()
                        )

                    }
                    else{
                        path.lineTo(
                            (offsetX + (pieceBitmap.width)/3 * 2).toFloat(),
                            pieceBitmap.height.toFloat()
                        )

                        path.cubicTo(
                            (offsetX + (pieceBitmap.width - offsetX)/ 6 * 5 ).toFloat(),
                            (pieceBitmap.height - bumpSize).toFloat(),
                            (offsetX + (pieceBitmap.width - offsetX)/ 6).toFloat(),
                            (pieceBitmap.height - bumpSize).toFloat(),
                            (offsetX + (pieceBitmap.width - offsetX)/ 3 ).toFloat(),
                            pieceBitmap.height.toFloat()

                        )
                        path.lineTo(
                            offsetX.toFloat(),
                            pieceBitmap.height.toFloat()
                        )

                    }

                    if (cols == 0 ){
                        path.close()
                    }
                    else{
                        path.lineTo(
                            offsetX.toFloat(),
                            (offsetY +( pieceBitmap.height - offsetY)/ 3 * 2).toFloat(),

                        )
                        path.cubicTo(
                            (offsetX - bumpSize).toFloat(),
                            (offsetY +( pieceBitmap.height)/6 * 5).toFloat(),
                            (offsetX - bumpSize).toFloat(),
                            (offsetY +( pieceBitmap.height)/6 ).toFloat(),
                            offsetX.toFloat(),
                            (offsetY +( pieceBitmap.height)/ 3).toFloat(),
                        )
                        path.close()
                    }

                    val paint = Paint()
                    paint.color = 0x10000000
                    paint.style = Paint.Style.FILL
                    canvas.drawPath(path,paint)
                    paint .xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    canvas.drawBitmap(pieceBitmap,0f , 0f, paint)

                    var border = Paint()
                    border.color = -0x7000001
                    border.style = Paint.Style.STROKE
                    border.strokeWidth = 3.0f
                    canvas.drawPath(path,border)



                    piece.setImageBitmap(puzzlePiece)
                    pieces.add(piece)
                    xCoord  += pieceWidth




                }
                yCoord += pieceHeight

            }

        }







        return pieces
    }

    fun checkGameOver(){
        if (isGameOver){
            AlertDialog.Builder(this@PuzzleActivity)
                .setTitle("You Won..!")
                .setIcon(R.drawable.ic_celebration)
                .setMessage("You Won \n if you want a new game ??")
                .setPositiveButton("Yess"){
                    dialog,_->
                    finish()
                    dialog.dismiss()
                }
                .setNegativeButton("No"){
                    dialog,_->
                    finish()
                    dialog.dismiss()
                }
                .create()
                .show()

        }
    }






    private val isGameOver :Boolean
    private get (){
        for (piece in pieces!!){
            if(piece.canMove){
                return false
            }
        }
        return true
    }

    private fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray {
        val ret = IntArray(4)
        if(imageView == null || imageView.drawable == null){
            return ret
        }
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        ret[2] = actW
        ret[3] = actH

        val imageViewW = imageView.width
        val imageViewH = imageView.height

        val top = (imageViewH -actH) / 2
        val left = (imageViewW -actW) / 2

        ret[0] = top
        ret[1] = left




        return ret
    }

    companion object{
        fun rotateImage(source:Bitmap,angle:Float):Bitmap{
            val matrix = Matrix()
            matrix.postRotate(angle)

            return Bitmap.createBitmap(source,0,0,source.width,source.height,matrix,true)

        }
    }
}