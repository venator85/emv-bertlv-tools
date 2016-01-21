package io.github.binaryfoo.controllers

import io.github.binaryfoo.DecodedData
import io.github.binaryfoo.EmvTags
import io.github.binaryfoo.QVsdcTags
import io.github.binaryfoo.decoders.DecodeSession
import io.github.binaryfoo.decoders.annotator.backgroundOf
import io.github.binaryfoo.hex.HexDumpElement
import org.hamcrest.collection.IsMapContaining.hasEntry
import org.hamcrest.collection.IsMapContaining.hasKey
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.ui.ModelMap

public class DecodeControllerTest {

    private var decodeController = DecodeController()
    private var mockMvc = MockMvcBuilders.standaloneSetup(decodeController).build()!!

    @Test
    public fun testPDOLShouldBeDecodedInGPOCommand() {
        val input = """00A4040007A000000003101000
6F318407A0000000031010A52650095649534120544553549F38189F66049F02069F03069F1A0295055F2A029A039C019F37049000
80A8000023832136000000000000001000000000000000003600000000000036120315000008E4C800
773E9F100706010A03A000005F2002202F57104761340000000043D1712201131838755F340111820200009F360200E29F2608271A23709BDE117E9F6C0200009000"""
        val modelMap = ModelMap()
        decodeController.decode("apdu-sequence", input.replace("\n", " "), "qVSDC", modelMap)
        val decodedData = modelMap["decodedData"]!! as List<DecodedData>
        val gpoCommand = findWithRaw(decodedData, "C-APDU: GPO")
        assertThat<DecodedData>(gpoCommand, `is`(not(nullValue())))
        val expectedDecodedTTQ = QVsdcTags.METADATA.get(QVsdcTags.TERMINAL_TX_QUALIFIERS).decoder.decode("36000000", 73, DecodeSession())
        assertThat(gpoCommand!!.children.first(), `is`(DecodedData(QVsdcTags.TERMINAL_TX_QUALIFIERS, "9F66 (TTQ - Terminal transaction qualifiers)", "36000000", 73, 77, expectedDecodedTTQ)))
        assertThat(gpoCommand.children.last(), `is`(DecodedData(EmvTags.UNPREDICTABLE_NUMBER, "9F37 (unpredictable number)", "0008E4C8", 102, 106, backgroundReading = backgroundOf("A nonce generated by the terminal for replay attack prevention"))))
        assertThat(modelMap, not(hasKey("rawData")))
    }

    @Test
    public fun testContactless() {
        val modelMap = ModelMap()
        val input = """00A4040007A000000004101000
6F1C8407A0000000041010A511500F505043204D434420303420207632309000
80A8000002830000
7716820259809410080101001001010118010200200102009000
00B2011400
7081B157115413330089600044D141220101234091725A0854133300896000445F200C455445432F504159504153535F24031412315F25030401015F280200565F3401018C219F02069F03069F090295055F2A029A039C019F37049F35019F45029F4C089F34038D0C910A8A0295059F37049F4C088E0C00000000000000005E031F039F0702FF009F080200029F0D0500000000009F0E0500000000009F0F0500000000009F420209789F4A0182DF4F031234569000
00B2011C00
7081FB9081F87AF7C3297510E795704B4B34BB1526EB953FE7BEE08DBF08DFBA056AA42FC726BC03373A319DDC6197E2F5B333401AE3E4BBFC5044D245BDDEBEA146994132D939D72E8E05CC958856DFCB33B26427812955B09F7A7A68AAAB6B8326AD2929082569DA040BD286F2F20A759E62683995D4B840655342C8088FA18806C2290992665A45EC16AC6FE4B0FA664FC644B7B485CD4612B2967B846A9482DB7D16E9F9A3A8295CE5D2B91718180F1E86E67E8E2275EB2116E651BD91AAC49D80D6E64DEB54977258465C09274DD7D232A98CD80C76458CEAAA2B0A414EE7A1B8899E6361B098307EBE5E87F31C41B0A32B248CF6DA9849810C49559000
00B2012400
703E8F01EF9F47030100019F32030100019F482A8F9498797B9DE111BF5EB97EF1820BA654E4867F09D6BB41BCB1E4FB3E9D287ABD67031918134731270792009000
00B2022400
80AE50002B000000001000000000000000000200000000000036120315000000AFDC22000000000000000000001F030000
7781B29F2701409F360200349F4B81906C9EC67BEF69A00E98F8ED96AAA2B945519E1348C78346B07D7650A9C2CC717611C69EDDBEFCAAF91FDEB09ABA2675CF430CCB0BAF0CAA8D8E18B9919790607847591970153565F65B33383C8757162669799D346B265B58421DD20E8109F5074AFB1B13B3A64D3470D8CC9E68342C8AAC238687B850EBEB260CB9F010AC4BD0F81C990026929974382077B103AD0C659F10120110904009248400000000000000000028FF9000"""
        decodeController.decode("apdu-sequence", input.replace("\n", " "), "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
        assertThat(modelMap, not(hasKey("rawData")))
        val decodedData = modelMap.get("decodedData") as List<DecodedData>
        assertThat(decodedData[0].hexDump!![0], `is`(HexDumpElement("00", 0)))
        assertThat(decodedData[0].hexDump!![1], `is`(HexDumpElement("A4", 1)))
        assertThat(decodedData[0].hexDump!![12], `is`(HexDumpElement("00", 12)))
        assertThat(decodedData[12].hexDump!![0], `is`(HexDumpElement("77", 652)))
        assertThat(decodedData[12].hexDump!![182], `is`(HexDumpElement("00", 834)))
    }

    @Test
    public fun testContactVisa() {
        val modelMap = ModelMap()
        val input = """00a4040007a000000003101000
6f168407a0000000031010a50b50095649534120544553549000
80a8000002830000
800e1c000801010010010100180304009000
00b2010c00
00000000
00b2011400
70625a0847613400000000508c159f02069f03069f1a0295055f2a029a039c019f37048d178a029f02069f03069f1a0295055f2a029a039c019f37045f24031712319f0702ff809f0802008c9f0d0500000000009f0e0500000000009f0f0500000000009000
00b2031c00
70128e100000000000000000410342031e031f029000
00b2041c00
70055f280207029000
00a4040007a000000003101000
6f168407a0000000031010a50b50095649534120544553549000
80a8000002830000
800e1c000801010010010100180304009000
00b2010c00
703857104761340000000050d1712201125234515f20104341524420352f5649534120544553549f1f10313235323330303435313030303030309000
00b2011400
70625a0847613400000000508c159f02069f03069f1a0295055f2a029a039c019f37048d178a029f02069f03069f1a0295055f2a029a039c019f37045f24031712319f0702ff809f0802008c9f0d0500000000009f0e0500000000009f0f0500000000009000
00b2031c00
70128e100000000000000000410342031e031f029000
00b2041c00
70055f280207029000
80ca9f1700
9f1701109000
80ae80001d00000000100000000000000000368000008000003612031600d3173a1f00
80228000297cb3ba090724c52406010a03a4b8000f112233445566778899aabbccddeeff9000
00820000081122334455667788
6700
80ae40001f303000000000100000000000000000368000008040003612031600d3173a1f00
8012400029bb31d191bced0cf206010a0364bc009000"""
        decodeController.decode("apdu-sequence", input.replace("\n", " "), "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testContactMastercard() {
        val modelMap = ModelMap()
        val input = """00a4040007a000000004101000
6f1c8407a0000000041010a511500f505043204d434420303420207632309000
80a8000002830000
00b2021400
70818f5a0854133300896000445f24031412315f25030401015f280200565f3401018c219f02069f03069f090295055f2a029a039c019f37049f35019f45029f4c089f34038d0c910a8a0295059f37049f4c088e0c00000000000000005e031f039f0702ff009f080200029f0d0500000000009f0e0500000000009f0f0500000000009f420209789f4a0182df4f031234569000
00b2011c00
7081fb9081f87af7c3297510e795704b4b34bb1526eb953fe7bee08dbf08dfba056aa42fc726bc03373a319ddc6197e2f5b333401ae3e4bbfc5044d245bddebea146994132d939d72e8e05cc958856dfcb33b26427812955b09f7a7a68aaab6b8326ad2929082569da040bd286f2f20a759e62683995d4b840655342c8088fa18806c2290992665a45ec16ac6fe4b0fa664fc644b7b485cd4612b2967b846a9482db7d16e9f9a3a8295ce5d2b91718180f1e86e67e8e2275eb2116e651bd91aac49d80d6e64deb54977258465c09274dd7d232a98cd80c76458ceaaa2b0a414ee7a1b8899e6361b098307ebe5e87f31c41b0a32b248cf6da9849810c49559000
00b2012400
703e8f01ef9f47030100019f32030100019f482a8f9498797b9de111bf5eb97ef1820ba654e4867f09d6bb41bcb1e4fb3e9d287abd67031918134731270792009000
00b2012c00
7081a657115413330089600044d1412201012340917293819021a0d15cce47a534b8c86f03fcb56da82332435bb2f7266a01196050d1c4a42c38c63f4b22331e7ba1b664ec925cb0539dc82539cb4f83818b90f6c94d4448d87f7140a3e3b0ad96c8c1ff62a9519f16ac4bf18f10e06a0366a1d6250752a59592bc61104efdd0dd332fd4760f3b922b169c18c31bdd706688b63b332b2da566e7176bd01c581e9fe45101ea49c1007a9000
00b2022c00
7081949f46819017abb31427eff0ccb678959f291c175bfe42581839603afbc34866f84966c11b29cd0b67a4e9d4eac1f18cc1be25d8d102bf4dee486e5fceecb731f347d57fd11bc96dd9cba154c2140edd69b0e1a18953255a3f91b6d4fb06e885702b958eba99714609fb40b881a0a69bc3112008bca9bbf55e1544e6f2d95ecb9c8656fb498767b5ead58171b32ec40a536d52906c9000
00a4040007a000000004101000
6f1c8407a0000000041010a511500f505043204d434420303420207632309000
80a8000002830000
7716820259009410100202011801010020010100280102009000
00b2021400
70818f5a0854133300896000445f24031412315f25030401015f280200565f3401018c219f02069f03069f090295055f2a029a039c019f37049f35019f45029f4c089f34038d0c910a8a0295059f37049f4c088e0c00000000000000005e031f039f0702ff009f080200029f0d0500000000009f0e0500000000009f0f0500000000009f420209789f4a0182df4f031234569000
00b2011c00
7081fb9081f87af7c3297510e795704b4b34bb1526eb953fe7bee08dbf08dfba056aa42fc726bc03373a319ddc6197e2f5b333401ae3e4bbfc5044d245bddebea146994132d939d72e8e05cc958856dfcb33b26427812955b09f7a7a68aaab6b8326ad2929082569da040bd286f2f20a759e62683995d4b840655342c8088fa18806c2290992665a45ec16ac6fe4b0fa664fc644b7b485cd4612b2967b846a9482db7d16e9f9a3a8295ce5d2b91718180f1e86e67e8e2275eb2116e651bd91aac49d80d6e64deb54977258465c09274dd7d232a98cd80c76458ceaaa2b0a414ee7a1b8899e6361b098307ebe5e87f31c41b0a32b248cf6da9849810c49559000
00b2012400
703e8f01ef9f47030100019f32030100019f482a8f9498797b9de111bf5eb97ef1820ba654e4867f09d6bb41bcb1e4fb3e9d287abd67031918134731270792009000
00b2012c00
7081a657115413330089600044d1412201012340917293819021a0d15cce47a534b8c86f03fcb56da82332435bb2f7266a01196050d1c4a42c38c63f4b22331e7ba1b664ec925cb0539dc82539cb4f83818b90f6c94d4448d87f7140a3e3b0ad96c8c1ff62a9519f16ac4bf18f10e06a0366a1d6250752a59592bc61104efdd0dd332fd4760f3b922b169c18c31bdd706688b63b332b2da566e7176bd01c581e9fe45101ea49c1007a9000
00b2022c00
7081949f46819017abb31427eff0ccb678959f291c175bfe42581839603afbc34866f84966c11b29cd0b67a4e9d4eac1f18cc1be25d8d102bf4dee486e5fceecb731f347d57fd11bc96dd9cba154c2140edd69b0e1a18953255a3f91b6d4fb06e885702b958eba99714609fb40b881a0a69bc3112008bca9bbf55e1544e6f2d95ecb9c8656fb498767b5ead58171b32ec40a536d52906c9000
80ae80002b000000001000000000000000000280000080000036120316008221f60122000000000000000000005e030000
77299f2701809f360200419f26084573436b1e4b95dd9f10120110a00009248400000000000000000029ff9000
80ae40001d11223344556677880000303080000080008221f601000000000000000000
77299f2701009f360200419f2608c74d18b08248fefc9f10120110201009248400000000000000000029ff9000"""
        decodeController.decode("apdu-sequence", input.replace("\n", " "), "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testMagStripe() {
        val modelMap = ModelMap()
        val input = """00A4040007A000000004101000
6F218407A0000000041010A516500A49443335312076312031BF0C075F5004746F746F9000
80A8000002830000
770A820200009404080101009000
00B2010C00
7081839F6C0200019F62060000000000079F6306000000000000563342353431333333303035363030333531315E4355535420494D50204D43203335312F5E313431323130313036373735303530309F6401009F650200079F660200009F6B115413330056003511D1412101067750500F9F6701009F680E00000000000000005E0342031F039000
802A8E80040000000000
770F9F610201F59F600201F59F3602000B9000"""
        decodeController.decode("apdu-sequence", input.replace("\n", " "), "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testDecodeFilledDOL() {
        val modelMap = ModelMap()
        decodeController.decode("filled-dol", "9F66049F02069F03069F1A0295055F2A029A039C019F3704:832136000000000000001000000000000000003600000000000036120315000008E4C8", "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testDecodeFilledDOLTwo() {
        val modelMap = ModelMap()
        decodeController.decode("filled-dol", "9F02069F03069F090295055F2A029A039C019F37049F35019F45029F4C089F3403:000000001000000000000000000200000000000036120315000000AFDC22000000000000000000001F0300", "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testInputWithConstructedTagThatCannotBeParsedAsConstructed() {
        val modelMap = ModelMap()
        val input = "F082013CE007D002432BD10101E9409F02060000000009009F03060000000000005F2A0200365F3601029A031203209F21031410389C01009F3704000A7CA39F1E0831393062346262319F1A020036E581E2EF6D9F0607A00000000310109F1B0400010000DF400400010000DF420400010000DF430100DF4401009F3501229F3303E060C09F40056000F0A0019F090200029F6D020002DF32039F6A049F15024444DF57050000000000DF56050000000000DF580500000000009F660436000000EF719F0607A00000000410109F1B0400010000DF400400010000DF410400010000DF420400010000DF430100DF4401009F3501229F3303E008889F40056000F0A0019F090200029F6D020002DF32039F6A049F15024444DF57050000000000DF5605CC50848800DF5805CC50848800DF610101E30AE0085F249F6C9F039F33"
        decodeController.decode("constructed", input, "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
        assertThat(modelMap, hasEntry(`is`("rawData"), `is`(not(nullValue()))))
        val hexDump = modelMap.get("rawData") as List<HexDumpElement>
        assertThat(hexDump[0], `is`(HexDumpElement("F0", 0)))
        assertThat(hexDump[319], `is`(HexDumpElement("33", 319)))
    }

    @Test
    public fun testGetProcessingOptionsWithTemplate80Reply() {
        val modelMap = ModelMap()
        val input = "80A8000002830000 800E1C000801010010010100180304009000"
        decodeController.decode("apdu-sequence", input, "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
        val decodedData = modelMap.get("decodedData") as List<DecodedData>
        assertThat(decodedData.get(1).children, `is`(DecodedAsStringMatcher.decodedAsString("""80 (Fixed response template): 1C00080101001001010018030400
  82 (AIP - Application Interchange Profile): 1C00
    1000 (Byte 1 Bit 5): Cardholder verification is supported
    0800 (Byte 1 Bit 4): Terminal risk management is to be performed
    0400 (Byte 1 Bit 3): Issuer authentication is supported
    0000 (Byte 2 Bit 8): Magstripe Mode Only Supported
  94 (AFL - Application File Locator): 080101001001010018030400
    SFI 1 record 1
    SFI 2 record 1
    SFI 3 records 3-4
""")))
    }

    @Test
    public fun testInvalidCvmResults() {
        val modelMap = ModelMap()
        val input = "F081D4E007D002522BD10101BF810105DF2D020000E50ADF91590120DF9158011FE9818C57104761340000000043D1712201131838758407A000000003101050095649534120544553549F260894961F236784C26D9F2701809F100706010A03A000009F36020134950500000000009F370400027D995F2A0200369C01209A030904029F02060000000020009F1A0200369F34033F0002820200005F3401115F2002202F9F6C0200009F660436000000E325E0239F03060000000000009F3303E060C09F1E0831393062346332629F3501229F09020002"
        decodeController.decode("constructed", input, "EMV", modelMap)
        assertThat(modelMap, hasEntry(`is`("decodedData"), `is`(not(nullValue()))))
    }

    @Test
    public fun testJson() {
        mockMvc.perform(get("/api/decode").param("tag", "95").param("value", "8000000000").param("meta", "EMV")).andReturn().getResponse().getContentAsString()
        //                .andExpect();

    }

    private fun findWithRaw(list: List<DecodedData>, wanted: String): DecodedData? = list.firstOrNull() { it.rawData.startsWith(wanted) }

}
