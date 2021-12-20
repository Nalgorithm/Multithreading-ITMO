/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author :TODO: Логвиненко Никита
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c3 by RegularInt(0)
    private var ci1 by RegularInt(0)
    private var ci2 by RegularInt(0)

    override fun write(time: Time) {
        //left -> right
        c1 = time.d1
        c2 = time.d2
        c3 = time.d3

        // right -> left
        ci2 = time.d2
        ci1 = time.d1
    }

    override fun read(): Time {
        //left -> right
        val r1 = ci1
        val r2 = ci2

        val ri3 = c3
        val ri2 = c2
        val ri1 = c1

        if (r1 == ri1) {
            if (r2 == ri2) {
                return Time(ri1, ri2, ri3)
            } else {
                return Time(ri1,ri2, 0)
            }
        }

        return Time(ri1, 0,0)
    }
}