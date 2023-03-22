package io.opentelemetry.demo.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.opentelemetry.demo.android.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val otel = OtelExamples()

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        otel.run()
        binding.buttonFirst.setOnClickListener { otel.sync() }
        binding.buttonSecond.setOnClickListener { otel.rxSync() }
        binding.buttonThird.setOnClickListener { otel.rxAsync() }
        binding.buttonFourth.setOnClickListener { otel.retrofit() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}