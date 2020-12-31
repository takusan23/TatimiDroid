package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoDetailBinding

/**
 * コメント一覧Fragmentと動画情報Fragmentを乗せるFragment
 * */
class JCNicoVideoDetailFragment : Fragment() {

    /** レイアウト。Fragmentを置くだけなんだけどね */
    private val nicovideoDetailBinding by lazy { FragmentNicovideoDetailBinding.inflate(layoutInflater) }

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return nicovideoDetailBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // NicoVideoInfoFragment 設置。親のFragmentManagerを利用することでViewModelを共通化できる。
        parentFragmentManager.beginTransaction().replace(nicovideoDetailBinding.fragmentNicovideoDetailInfoFragmentFrameLayout.id, JCNicoVideoInfoFragment()).commit()

        // コメント一覧Fragment設置。
        parentFragmentManager.beginTransaction().replace(nicovideoDetailBinding.fragmentNicovideoDetailCommentFragmentFrameLayout.id, NicoVideoCommentFragment()).commit()

        // コメント一覧展開など
        val bottomSheet = BottomSheetBehavior.from(nicovideoDetailBinding.fragmentNicovideoDetailCommentFragmentFrameLayout)
        viewModel.commentListBottomSheetLiveData.observe(viewLifecycleOwner) { state ->
            bottomSheet.state = state
        }

    }
}