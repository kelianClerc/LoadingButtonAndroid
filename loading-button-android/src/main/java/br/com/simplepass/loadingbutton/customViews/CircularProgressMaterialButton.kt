package br.com.simplepass.loadingbutton.customViews

import android.animation.AnimatorSet
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import br.com.simplepass.loadingbutton.R
import br.com.simplepass.loadingbutton.animatedDrawables.CircularProgressAnimatedDrawable
import br.com.simplepass.loadingbutton.animatedDrawables.CircularRevealAnimatedDrawable
import br.com.simplepass.loadingbutton.animatedDrawables.ProgressType
import br.com.simplepass.loadingbutton.disposeAnimator
import br.com.simplepass.loadingbutton.presentation.ProgressButtonPresenter
import br.com.simplepass.loadingbutton.presentation.State
import br.com.simplepass.loadingbutton.utils.addLifecycleObserver
import com.google.android.material.button.MaterialButton

open class CircularProgressMaterialButton : MaterialButton, ProgressButton {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet? = null, defStyleAttr: Int = 0) {
        val typedArray: TypedArray? = attrs?.run {
            getContext().obtainStyledAttributes(
                this,
                R.styleable.CircularProgressMaterialButton,
                defStyleAttr,
                0
            )
        }

        typedArray?.let { tArray ->
            buttonTint = tArray.getColorStateList(
                R.styleable.CircularProgressMaterialButton_material_backgroundTint
            )
            spinnerTint = tArray.getColorStateList(
                R.styleable.CircularProgressMaterialButton_material_spinning_bar_tint
            )
            config(tArray)
        }

        buttonTint?.let { backgroundTintList = it }

        typedArray?.recycle()

        // all ProgressButton instances implement LifecycleObserver, so we can
        // auto-register each instance on initialization
        getContext().addLifecycleObserver(this)
    }

    private fun config(tArray: TypedArray) {
        initialCorner = tArray.getDimension(
            R.styleable.CircularProgressMaterialButton_material_initialCornerAngle,
            0f
        )
        finalCorner = tArray.getDimension(
            R.styleable.CircularProgressMaterialButton_material_finalCornerAngle,
            100f
        )

        spinningBarWidth = tArray.getDimension(
            R.styleable.CircularProgressMaterialButton_material_spinning_bar_width,
            10f
        )
        spinningBarColor = spinnerTint?.defaultColor ?: tArray.getColor(
            R.styleable.CircularProgressMaterialButton_material_spinning_bar_color,
            spinningBarColor
        )

        paddingProgress = tArray.getDimension(
            R.styleable.CircularProgressMaterialButton_material_spinning_bar_padding,
            0F
        )
    }

    private var buttonTint: ColorStateList? = null
    private var spinnerTint: ColorStateList? = null

    override var paddingProgress = 0F

    override var spinningBarWidth = 10F
    override var spinningBarColor = ContextCompat.getColor(context, android.R.color.black)

    override var finalCorner = 0F
    override var initialCorner = 0F

    private lateinit var initialState: InitialState

    override val finalHeight: Int by lazy { height }
    private val initialHeight: Int by lazy { height }
    override val finalWidth: Int by lazy {
        val padding = Rect()
        drawableBackground.getPadding(padding)
        finalHeight - (Math.abs(padding.top - padding.left) * 2)
    }

    override var progressType: ProgressType
        get() = progressAnimatedDrawable.progressType
        set(value) {
            progressAnimatedDrawable.progressType = value
        }

    override var drawableBackground: Drawable = background

    private var savedAnimationEndListener: () -> Unit = {}

    private val presenter = ProgressButtonPresenter(this)

    private val morphAnimator by lazy {
        AnimatorSet().apply {
            playTogether(
                cornerAnimator(
                    this@CircularProgressMaterialButton,
                    initialCorner,
                    finalCorner
                ),
                widthAnimator(
                    this@CircularProgressMaterialButton,
                    initialState.initialWidth,
                    finalWidth
                ),
                heightAnimator(
                    this@CircularProgressMaterialButton,
                    initialHeight,
                    finalHeight
                )
            )

            addListener(morphListener(presenter::morphStart, presenter::morphEnd))
        }
    }

    private val morphRevertAnimator by lazy {
        AnimatorSet().apply {
            playTogether(
                cornerAnimator(
                    this@CircularProgressMaterialButton,
                    finalCorner,
                    initialCorner
                ),
                widthAnimator(
                    this@CircularProgressMaterialButton,
                    finalWidth,
                    initialState.initialWidth
                ),
                heightAnimator(
                    this@CircularProgressMaterialButton,
                    finalHeight,
                    initialHeight
                )
            )

            addListener(morphListener(presenter::morphRevertStart, presenter::morphRevertEnd))
        }
    }

    private val progressAnimatedDrawable: CircularProgressAnimatedDrawable by lazy {
        createProgressDrawable()
    }

    private lateinit var revealAnimatedDrawable: CircularRevealAnimatedDrawable

    override fun getState(): State = presenter.state

    override fun saveInitialState() {
        initialState = InitialState(width)
    }

    override fun recoverInitialState() {}

    override fun hideInitialState() {}

    override fun drawProgress(canvas: Canvas) {
        progressAnimatedDrawable.drawProgress(canvas)
    }

    override fun drawDoneAnimation(canvas: Canvas) {
        revealAnimatedDrawable.draw(canvas)
    }

    override fun startRevealAnimation() {
        revealAnimatedDrawable.start()
    }

    override fun startMorphAnimation() {
        applyAnimationEndListener(morphAnimator, savedAnimationEndListener)
        morphAnimator.start()
    }

    override fun startMorphRevertAnimation() {
        applyAnimationEndListener(morphAnimator, savedAnimationEndListener)
        morphRevertAnimator.start()
    }

    override fun stopProgressAnimation() {
        progressAnimatedDrawable.stop()
    }

    override fun stopMorphAnimation() {
        morphAnimator.end()
    }

    override fun startAnimation(onAnimationEndListener: () -> Unit) {
        savedAnimationEndListener = onAnimationEndListener
        presenter.startAnimation()
    }

    override fun revertAnimation(onAnimationEndListener: () -> Unit) {
        savedAnimationEndListener = onAnimationEndListener
        presenter.revertAnimation()
    }

    override fun stopAnimation() {
        presenter.stopAnimation()
    }

    override fun doneLoadingAnimation(fillColor: Int, bitmap: Bitmap) {
        presenter.doneLoadingAnimation(fillColor, bitmap)
    }

    override fun initRevealAnimation(fillColor: Int, bitmap: Bitmap) {
        revealAnimatedDrawable = createRevealAnimatedDrawable(fillColor, bitmap)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun dispose() {
        morphAnimator.disposeAnimator()
        morphRevertAnimator.disposeAnimator()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        presenter.onDraw(canvas)
    }

    override fun setProgress(value: Float) {
        if (presenter.validateSetProgress()) {
            progressAnimatedDrawable.progress = value
        } else {
            throw IllegalStateException("Set progress in being called in the wrong state: ${presenter.state}." +
                " Allowed states: ${State.PROGRESS}, ${State.MORPHING}, ${State.WAITING_PROGRESS}")
        }
    }

    override fun setCompoundDrawables(left: Drawable?, top: Drawable?, right: Drawable?, bottom: Drawable?) {}

    data class InitialState(var initialWidth: Int)
}
