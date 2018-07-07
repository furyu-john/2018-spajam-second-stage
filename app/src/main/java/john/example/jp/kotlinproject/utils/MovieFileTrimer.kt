package john.example.jp.kotlinproject.utils

import android.os.Environment
import android.util.Log
import com.coremedia.iso.boxes.Container
import com.coremedia.iso.boxes.MovieHeaderBox
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack
import com.googlecode.mp4parser.util.Matrix
import com.googlecode.mp4parser.util.Path
import java.io.File
import java.io.FileOutputStream
import java.util.*

object MovieFileTrimer {
    /**
     * srcFileとdestFileは存在しなかったらエラー
     * 動画の長さが足りなければエラー
     * 保存に成功した場合trueを返す
     */
    fun test(srcFile: File, destFile: File, startMs: Int, endMs: Int): Boolean {
        val movieFile = FileDataSourceImpl(srcFile)
        val movie: Movie = MovieCreator.build(movieFile)

        val tracks = movie.tracks
        movie.tracks = LinkedList()
        val startTime:Double = (startMs.toDouble() / 1000.toDouble())
        val endTime:Double = (endMs.toDouble() / 1000.toDouble())

        for (track: Track in tracks) {
            var currentSample: Long = 0
            var currentTime = 0.0
            var startSample: Long = -1
            var endSample: Long = -1
            for (i in 0 until track.sampleDurations.size) {
                if (currentTime <= startTime) {

                    // current sample is still before the new starttime
                    startSample = currentSample
                }
                if (currentTime <= endTime) {
                    // current sample is after the new start time and still
                    // before the new endtime
                    endSample = currentSample
                } else {
                    // current sample is after the end of the cropped video
                    break
                }
                currentTime += track.sampleDurations[i].toDouble() / track.trackMetaData.timescale.toDouble()
                currentSample++
            }
            movie.addTrack(CroppedTrack(track, startSample, endSample))
        }

        // 保存する
        val out: Container = DefaultMp4Builder().build(movie)
        val mvhd: MovieHeaderBox = Path.getPath(out, "moov/mvhd")
        mvhd.setMatrix(Matrix.ROTATE_180)
        val fos = FileOutputStream(destFile)
        val fc = fos.getChannel()
        try {
            out.writeContainer(fc)
            return true
        } finally {
            fc.close()
            fos.close()
            movieFile.close()
        }
    }
}