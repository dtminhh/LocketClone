package com.mainpack.locketcameraclone.ui.fragment.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentSignInBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListener()
    }

    private fun setUpListener() {
        binding.apply {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        signInBackBtn.performClick()
                    }
                })
            signInContinueBtn.setOnClickListener {
                goToPasswordScreen()
            }

            signInConfirm.setOnClickListener {
                findNavController().navigate(R.id.action_signInFragment_to_cameraFragment)
            }

            signInBackBtn.setOnClickListener {
                when (signInFlipper.displayedChild) {
                    1 ->
                        backToEmailScreen()

                    2 ->
                        goToPasswordScreen()

                    else -> findNavController().popBackStack()
                }
            }
        }
    }

    private fun goToPasswordScreen() {
        binding.apply {
            passwordSignInEdt.text = null
            passwordSignInEdt.requestFocus()
            signInFlipper.setInAnimation(context, R.anim.slide_in_right_navigator)
            signInFlipper.displayedChild = 1
        }
    }

    private fun backToEmailScreen() {
        binding.apply {
            signInEmailEdt.text = null
            signInEmailEdt.requestFocus()
            signInFlipper.setInAnimation(context, R.anim.slide_out_left_navigator)
            signInFlipper.displayedChild = 0
        }
    }
}