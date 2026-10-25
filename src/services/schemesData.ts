import { GovernmentScheme, SchemeNotification, SchemeCategory } from '../types';

export const VERIFIED_GOVERNMENT_SCHEMES: GovernmentScheme[] = [
  {
    id: 'adip-scheme',
    name: 'ADIP Scheme (Assistance to Disabled Persons for Purchase/Fitting of Aids and Appliances)',
    nativeNames: {
      en: 'ADIP Scheme for Assistive Aids & Devices',
      hi: 'एडिप योजना - दिव्यांगजनों के लिए सहायक उपकरण सहायता',
      mr: 'अॅडिप योजना - दिव्यांग व्यक्तींना सहाय्यक उपकरणे वाटप',
    },
    shortDescription: {
      en: 'Provides free high-tech assistive devices like Smart White Canes, DAISY Digital Audio Book Players, Screen-Reading Accessible Laptops, and Braille Kits to visually impaired persons.',
      hi: 'दृष्टिबाधित व्यक्तियों को स्मार्ट व्हाइट केन, डेज़ी डिजिटल प्लेयर, स्क्रीन-रीडिंग लैपटॉप और ब्रेल किट जैसे उच्च-तकनीकी उपकरण निःशुल्क प्रदान करता है।',
      mr: 'दृष्टिहीन व्यक्तींना स्मार्ट व्हाईट केन, डेझी ऑडिओ बुक प्लेअर, स्क्रीन-रीडिंग लॅपटॉप आणि ब्रेल किट मोफत पुरवते.',
    },
    authority: 'Department of Empowerment of Persons with Disabilities (DEPwD), Ministry of Social Justice & Empowerment, Government of India & ALIMCO',
    jurisdiction: 'Central (Government of India)',
    category: 'assistive_devices',
    eligibility: [
      'Indian citizen with certified 40% or more visual impairment (Blindness or Low Vision) verified via UDID Card or Medical Authority.',
      'Monthly family income up to ₹30,000 for 100% full financial subsidy (completely free aids).',
      'Monthly family income between ₹30,001 and ₹45,000 for 50% aid subsidy.',
      'Should not have received the same assistive device within the last 3 years (1 year for students under 19).',
    ],
    benefits: [
      'Free Smart Ultrasonic Navigation Cane (detects obstacles up to 3 meters).',
      'Free DAISY Audio Book Player & Accessible Voice Recorder for blind students & professionals.',
      'Braille Slate, Stylus, Taylor Frame, and Talking Watch.',
      'For college/higher education students: Accessible Screen-Reader Laptop / Tablet with NVDA/JAWS.',
    ],
    documentsRequired: [
      'Unique Disability Identity (UDID) Card / Swavlamban Card (or Disability Certificate).',
      'Income Certificate issued by Revenue Authority / Tehsildar (or BPL Ration Card / Salary Slip).',
      'Aadhaar Card or proof of identity and address.',
      'Two recent passport-size photographs showing disability.',
    ],
    applicationOpeningDate: '2026-04-01',
    applicationClosingDate: '2027-03-31',
    officialUrl: 'https://disabilityaffairs.gov.in',
    applicationInstructions: [
      'Apply online via the Department of Empowerment of Persons with Disabilities portal (disabilityaffairs.gov.in) or ALIMCO portal.',
      'Or register in person at district-level ALIMCO Assessment and Distribution Camps organized with the District Social Welfare Office.',
      'Keep your UDID card number ready for instant verification.',
    ],
    lastVerified: 'October 2026 (Verified via Official DEPwD Gazette)',
    isFeatured: true,
  },
  {
    id: 'sanjay-gandhi-pension-maharashtra',
    name: 'Sanjay Gandhi Niradhar Anudan Yojana (Divyang Monthly Pension Scheme)',
    nativeNames: {
      en: 'Sanjay Gandhi Niradhar Anudan Yojana (Disability Pension)',
      hi: 'संजय गांधी निराधार अनुदान योजना (दिव्यांग मासिक पेंशन)',
      mr: 'संजय गांधी निराधार अनुदान योजना (दिव्यांग निवृत्तीवेतन)',
    },
    shortDescription: {
      en: 'Maharashtra State Government monthly financial pension of ₹1,500 deposited directly into the bank account of visually impaired and severely disabled citizens.',
      hi: 'महाराष्ट्र सरकार द्वारा दृष्टिबाधित व गंभीर दिव्यांग नागरिकों के बैंक खाते में प्रतिमाह ₹1,500 की सीधी वित्तीय सहायता।',
      mr: 'महाराष्ट्र शासनातर्फे दृष्टिहीन आणि गंभीर दिव्यांग नागरिकांच्या थेट बँक खात्यात दरमहा ₹१,५०० चे आर्थिक साहाय्य.',
    },
    authority: 'Social Justice and Special Assistance Department, Government of Maharashtra',
    jurisdiction: 'State (Maharashtra)',
    category: 'pension',
    eligibility: [
      'Permanent domicile of Maharashtra State for at least 15 continuous years.',
      'Certified visual impairment (blindness or low vision) of 40% or above.',
      'Annual family income ceiling not exceeding ₹50,000.',
      'Age between 18 and 65 years (senior citizens automatically transition to Shravanbal Yojana).',
    ],
    benefits: [
      '₹1,500 monthly pension transferred directly via Direct Benefit Transfer (DBT) to the beneficiary Bank/Post Office account.',
      'Additional monthly family dependent allowance of ₹250 per child if children are dependent.',
    ],
    documentsRequired: [
      'Disability Medical Certificate / UDID Card showing minimum 40% blindness.',
      'Maharashtra 15-Year Domicile Certificate or Residential Proof.',
      'Income Certificate issued by Tehsildar / Competent Revenue Authority.',
      'Bank Account Passbook with Aadhaar seeding and IFSC code.',
      'Age Proof (Birth Certificate / School Leaving / Aadhaar).',
    ],
    applicationOpeningDate: 'Continuous (Open all year round)',
    applicationClosingDate: 'Open indefinitely',
    officialUrl: 'https://sanjaygandhianudan.maharashtra.gov.in',
    applicationInstructions: [
      'Obtain Form A from the local Tehsildar / Taluk Social Welfare Office or download from the portal.',
      'Submit the verified application along with medical disability proof and income certificate to the Tehsildar committee.',
      'Sanction orders are issued within 30 days of approval.',
    ],
    lastVerified: 'October 2026 (Government of Maharashtra Social Welfare Notification)',
    isFeatured: true,
  },
  {
    id: 'national-scholarship-pwd',
    name: 'Pre-Matric & Post-Matric Scholarships for Students with Disabilities',
    nativeNames: {
      en: 'National Scholarships for Students with Disabilities (NSP)',
      hi: 'दिव्यांग छात्रों के लिए राष्ट्रीय छात्रवृत्ति (एनएसपी)',
      mr: 'दिव्यांग विद्यार्थ्यांसाठी राष्ट्रीय शिष्यवृत्ती योजना (एनएसपी)',
    },
    shortDescription: {
      en: 'Central government educational scholarships covering full tuition fees, reader allowance, book grants, and assistive computer allowances for visually impaired school and college students.',
      hi: 'दृष्टिबाधित स्कूली व कॉलेज छात्रों के लिए पूर्ण ट्यूशन फीस, रीडर भत्ता, पुस्तक अनुदान और कंप्यूटर सहायता देने वाली केंद्र सरकार की छात्रवृत्ति।',
      mr: 'दृष्टिहीन शालेय आणि महाविद्यालयीन विद्यार्थ्यांसाठी पूर्ण शैक्षणिक शुल्क, वाचक भत्ता आणि पुस्तक अनुदान देणारी राष्ट्रीय शिष्यवृत्ती.',
    },
    authority: 'Department of Empowerment of Persons with Disabilities, Ministry of Social Justice & Empowerment, Govt of India',
    jurisdiction: 'Central (Government of India)',
    category: 'scholarship',
    eligibility: [
      'Regular full-time students in recognized schools (Class 9-10 for Pre-Matric) and Class 11 to Post-Graduation (Post-Matric) with 40%+ visual impairment.',
      'Annual family income from all sources not exceeding ₹2,50,000 per annum.',
      'Applicant must not hold any other central government scholarship during the academic session.',
    ],
    benefits: [
      'Maintenance allowance: ₹1,600/month for hostellers and ₹750/month for day scholars.',
      'Special Reader Allowance for Blindness: ₹2,000/month for visual impairment assistance.',
      'Book grant and annual study material reimbursement up to ₹4,000/year.',
      'Computer & screen-reading assistive software purchase grant up to ₹30,000.',
    ],
    documentsRequired: [
      'Valid UDID Disability Certificate showing 40%+ visual impairment.',
      'Previous Academic Year Marksheet with minimum passing marks.',
      'Annual Family Income Certificate.',
      'Current Institute Fee Receipt and Bonafide Student Certificate.',
      'Active Student Bank Account linked with Aadhaar.',
    ],
    applicationOpeningDate: '2026-07-15',
    applicationClosingDate: '2026-11-30',
    officialUrl: 'https://scholarships.gov.in',
    applicationInstructions: [
      'Register on the National Scholarship Portal (scholarships.gov.in) with your Aadhaar / UDID number.',
      'Select Department of Empowerment of Persons with Disabilities scholarship schemes.',
      'Upload verified documents and submit for Institute verification before the deadline.',
    ],
    lastVerified: 'October 2026 (Verified via National Scholarship Portal Bulletin)',
    isFeatured: true,
  },
  {
    id: 'udid-swavlamban-card',
    name: 'Unique Disability ID (UDID) / Swavlamban Card Project',
    nativeNames: {
      en: 'Unique Disability ID (UDID) / Swavlamban Card',
      hi: 'यूडीआईडी कार्ड / स्वावलंबन कार्ड परियोजना',
      mr: 'युडीआयडी (UDID) स्वावलंबन कार्ड प्रकल्प',
    },
    shortDescription: {
      en: 'The universal digital smart card that serves as verified national proof of disability for railway concessions, bus travel, healthcare, tax exemptions, and government welfare schemes.',
      hi: 'एकल डिजिटल स्मार्ट कार्ड जो रेलवे रियायत, मुफ्त बस यात्रा, चिकित्सा और सभी सरकारी योजनाओं के लिए राष्ट्रीय स्तर पर मान्य प्रमाण है।',
      mr: 'रेल्वे सवलत, मोफत बस प्रवास, आरोग्य सुविधा आणि सर्व शासकीय योजनांसाठी अधिकृत राष्ट्रीय डिजिटल स्मार्ट कार्ड.',
    },
    authority: 'Department of Empowerment of Persons with Disabilities, Ministry of Social Justice & Empowerment, Govt of India',
    jurisdiction: 'National / DEPwD',
    category: 'central',
    eligibility: [
      'Any Indian citizen with permanent or temporary visual impairment (Blindness / Low Vision) assessed by a government Medical Board.',
    ],
    benefits: [
      'Single national card accepted across all states, railways, airlines, and government departments.',
      'Automatic eligibility for 75% Indian Railways travel concession for visual impairment.',
      'Paperless access to government welfare, assistive aids under ADIP, and scholarships.',
      'Contains QR code with medical assessment and validity for easy scanning.',
    ],
    documentsRequired: [
      'Proof of Identity (Aadhaar Card, Voter ID, or PAN).',
      'Proof of Address.',
      'Existing Medical Certificate of Disability (if available) or Medical Board appointment slip.',
      'Clear passport photograph.',
    ],
    applicationOpeningDate: 'Continuous (24x7 Digital Service)',
    applicationClosingDate: 'Permanent National Scheme',
    officialUrl: 'https://www.swavlambancard.gov.in',
    applicationInstructions: [
      'Visit swavlambancard.gov.in and click "Apply for Disability Certificate & UDID Card".',
      'Fill in personal, disability, and address details and select your district civil hospital for medical board verification.',
      'Track application status online and download the e-UDID card immediately after approval.',
    ],
    lastVerified: 'October 2026 (DEPwD National Portal)',
    isFeatured: true,
  },
  {
    id: 'msrtc-bus-concession-maharashtra',
    name: 'MSRTC 100% Free Bus Travel Concession for Blind Persons',
    nativeNames: {
      en: 'Maharashtra State (MSRTC) Free Bus Travel Concession',
      hi: 'महाराष्ट्र राज्य परिवहन (एसटी) दृष्टिबाधित निःशुल्क बस यात्रा',
      mr: 'एसटी महामंडळ (MSRTC) दृष्टिहीन मोफत बस प्रवास सवलत',
    },
    shortDescription: {
      en: 'Maharashtra State Road Transport Corporation provides 100% free bus fare across all ordinary, semi-luxury, and express buses for totally blind persons, and 50% discount for an escort/companion.',
      hi: 'महाराष्ट्र राज्य सड़क परिवहन निगम द्वारा दृष्टिबाधित व्यक्तियों को सभी साधारण व एक्सप्रेस बसों में 100% मुफ्त यात्रा तथा साथ चलने वाले सहायक को 50% छूट।',
      mr: 'एसटी महामंडळाच्या सर्व साध्या, निमआराम व एक्सप्रेस बसेसमध्ये पूर्ण दृष्टिहीन प्रवाशांना १००% मोफत प्रवास आणि सोबतच्या एका साथीदारास ५०% सवलत.',
    },
    authority: 'Maharashtra State Road Transport Corporation (MSRTC) & Dept of Social Justice, Maharashtra',
    jurisdiction: 'State (Maharashtra)',
    category: 'maharashtra',
    eligibility: [
      'Resident of Maharashtra with 100% visual impairment / total blindness certificate or UDID card.',
      'Valid MSRTC Smart Travel Card issued by any MSRTC bus depot in Maharashtra.',
    ],
    benefits: [
      '100% concession on passenger fare across all MSRTC ordinary, express, and night service buses statewide.',
      '50% passenger fare concession for one escort/companion accompanying the visually impaired passenger.',
      'Priority seating reservations in the front seats of all state transport buses.',
    ],
    documentsRequired: [
      'Disability Certificate showing 100% blindness / UDID card.',
      'Aadhaar Card proving Maharashtra residence.',
      'Two passport photos.',
      'MSRTC Concession Card application form available at local bus depot.',
    ],
    applicationOpeningDate: 'Open Year-Round',
    applicationClosingDate: 'Continuous',
    officialUrl: 'https://msrtc.maharashtra.gov.in',
    applicationInstructions: [
      'Visit your nearest MSRTC Divisional Office or bus depot with original UDID/medical disability certificate.',
      'Submit the pass application form with ₹50 smart card administrative fee.',
      'The Smart Card pass is issued within 7 working days, valid for lifetime or certificate duration.',
    ],
    lastVerified: 'October 2026 (MSRTC Transport Guidelines)',
    isFeatured: false,
  },
  {
    id: 'divyangjan-swavalamban-nhfdc',
    name: 'Divyangjan Swavalamban Yojana (Concessional Loan & Self-Employment)',
    nativeNames: {
      en: 'Divyangjan Swavalamban Yojana (Concessional Loan)',
      hi: 'दिव्यांगजन स्वावलंबन योजना - स्वरोजगार ऋण सहायता',
      mr: 'दिव्यांगजन स्वावलंबन योजना - स्वयंरोजगार कर्ज योजना',
    },
    shortDescription: {
      en: 'Provides low-interest concessional business and educational loans up to ₹50 Lakh with interest subsidies for visually impaired entrepreneurs and vocational professionals.',
      hi: 'दृष्टिबाधित उद्यमियों व व्यवसायियों को ₹50 लाख तक का रियायती ब्याज दर (5% से 8%) पर स्वरोजगार व उच्च शिक्षा ऋण।',
      mr: 'दृष्टिहीन उद्योजक व सुशिक्षित तरुणांना ५% ते ८% सवलतीच्या व्याजदराने ₹५० लाखांपर्यंत स्वयंरोजगार व उच्च शिक्षण कर्ज.',
    },
    authority: 'National Divyangjan Finance and Development Corporation (NDFDC / NHFDC), Govt of India',
    jurisdiction: 'Central (Government of India)',
    category: 'central',
    eligibility: [
      'Indian citizen with 40% or more certified visual impairment.',
      'Age between 18 and 55 years for business ventures; 18-35 for higher education loans.',
      'Relevant project proposal or admission letter for vocational/higher education course.',
    ],
    benefits: [
      'Loan assistance up to ₹50 Lakh for service/manufacturing business, clinic, IT center, or retail store.',
      'Special 0.5% to 1% additional interest rebate for visually impaired women entrepreneurs.',
      'No collateral requirement for loans up to ₹5 Lakh under credit guarantee scheme.',
    ],
    documentsRequired: [
      'UDID Card / Disability Certificate.',
      'Project Feasibility Report / Business Plan.',
      'PAN Card, Aadhaar Card, and Bank Account statement of past 6 months.',
      'Educational qualifications / vocational training certificates.',
    ],
    applicationOpeningDate: 'Open throughout financial year',
    applicationClosingDate: 'Continuous',
    officialUrl: 'https://www.nhfdc.nic.in',
    applicationInstructions: [
      'Apply online via the NDFDC portal (nhfdc.nic.in) or through State Channelising Agencies (e.g., MPBCDC in Maharashtra).',
      'The designated state agency conducts appraisal and forwards approved loans to partner banks for disbursement.',
    ],
    lastVerified: 'October 2026 (NDFDC Official Circular)',
    isFeatured: false,
  },
  {
    id: 'mahadbt-disability-scholarship',
    name: 'Maharashtra State Scholarship for Disabled Students (MahaDBT)',
    nativeNames: {
      en: 'Maharashtra State Scholarship for Disabled Students (MahaDBT)',
      hi: 'महाराष्ट्र राज्य दिव्यांग विद्यार्थी छात्रवृत्ति (महाडीबीटी)',
      mr: 'महाराष्ट्र शासन दिव्यांग विद्यार्थी मॅट्रिकोत्तर शिष्यवृत्ती (महाडीबीटी)',
    },
    shortDescription: {
      en: 'Post-matric state scholarship reimbursing full tuition fees, examination fees, and monthly maintenance allowance for visually impaired students enrolled in Maharashtra colleges.',
      hi: 'महाराष्ट्र के कॉलेजों में अध्ययनरत दृष्टिबाधित छात्रों की पूर्ण ट्यूशन व परीक्षा फीस प्रतिपूर्ति तथा मासिक भत्ता योजना।',
      mr: 'महाराष्ट्रातील महाविद्यालयांमध्ये शिकणाऱ्या दृष्टिहीन विद्यार्थ्यांसाठी संपूर्ण शिक्षण व परीक्षा शुल्क प्रतिपूर्ती आणि मासिक भत्ता.',
    },
    authority: 'Social Justice and Special Assistance Department, Govt of Maharashtra',
    jurisdiction: 'State (Maharashtra)',
    category: 'scholarship',
    eligibility: [
      'Maharashtra domicile student with 40%+ visual impairment.',
      'Studying in a recognized diploma, degree, post-graduate, or professional institute in Maharashtra.',
      'Annual family income up to ₹2,50,000 for full fee reimbursement.',
    ],
    benefits: [
      '100% waiver/reimbursement of college tuition, lab, library, and university exam fees.',
      'Monthly maintenance allowance ranging from ₹1,000 to ₹1,500.',
      'Hostel accommodation fee reimbursement for outstation students.',
    ],
    documentsRequired: [
      'UDID Card / Disability Certificate.',
      'Maharashtra Domicile Certificate.',
      'Income Certificate from Tehsildar.',
      'College Bonafide Certificate & Fee Structure.',
      'Aadhaar linked Bank Account details.',
    ],
    applicationOpeningDate: '2026-08-01',
    applicationClosingDate: '2026-12-15',
    officialUrl: 'https://mahadbt.maharashtra.gov.in',
    applicationInstructions: [
      'Create an account on the MahaDBT portal (mahadbt.maharashtra.gov.in).',
      'Navigate to Social Justice & Special Assistance Department -> Scholarship for Persons with Disabilities.',
      'Upload the required certificates and submit for College Principal verification.',
    ],
    lastVerified: 'October 2026 (MahaDBT Portal Notification)',
    isFeatured: false,
  },
  {
    id: 'nfpwd-fellowship',
    name: 'National Fellowship for Persons with Disabilities (NFPwD)',
    nativeNames: {
      en: 'National Fellowship for Persons with Disabilities (NFPwD)',
      hi: 'दिव्यांगजनों के लिए राष्ट्रीय शोध फेलोशिप (एनएफपीडब्ल्यूडी)',
      mr: 'दिव्यांग विद्यार्थ्यांसाठी राष्ट्रीय संशोधन फेलोशिप (NFPwD)',
    },
    shortDescription: {
      en: 'Prestigious national research fellowship providing ₹37,000/month JRF and ₹42,000/month SRF plus contingency grants for visually impaired scholars pursuing M.Phil and Ph.D degrees.',
      hi: 'एम.फिल व पीएचडी कर रहे दृष्टिबाधित शोधार्थियों के लिए ₹37,000/माह (जेआरएफ) और ₹42,000/माह (एसआरएफ) की राष्ट्रीय फेलोशिप।',
      mr: 'एम.फिल व पीएच.डी. करणाऱ्या दृष्टिहीन संशोधक विद्यार्थ्यांसाठी दरमहा ₹३७,००० (JRF) आणि ₹४२,००० (SRF) ची राष्ट्रीय संशोधन शिष्यवृत्ती.',
    },
    authority: 'University Grants Commission (UGC) & DEPwD, Government of India',
    jurisdiction: 'Central (Government of India)',
    category: 'scholarship',
    eligibility: [
      'Visually impaired candidates with 40%+ disability enrolled in regular full-time M.Phil/Ph.D at an Indian recognized university.',
      'Must have qualified UGC-NET or CSIR-NET (eligibility for lectureship).',
    ],
    benefits: [
      'Junior Research Fellowship (JRF): ₹37,000 per month for the first two years.',
      'Senior Research Fellowship (SRF): ₹42,000 per month for remaining tenure.',
      'Contingency grant up to ₹25,000 per annum for humanities and ₹28,000 for science.',
      'Escort / Reader assistance allowance of ₹3,000 per month specifically for visually impaired scholars.',
    ],
    documentsRequired: [
      'UDID Disability Card.',
      'Ph.D/M.Phil Admission & Registration Letter.',
      'UGC-NET / CSIR-NET Score Card.',
      'Post-Graduation Degree Certificate with minimum 50% marks.',
    ],
    applicationOpeningDate: '2026-09-01',
    applicationClosingDate: '2026-10-31',
    officialUrl: 'https://ugcnet.nta.ac.in',
    applicationInstructions: [
      'Applications are invited annually on the UGC National Fellowship portal.',
      'Fill in research scholar enrolment details and upload university supervisor verification.',
    ],
    lastVerified: 'October 2026 (UGC Official Portal Gazette)',
    isFeatured: false,
  },
  {
    id: 'savitribai-phule-divyang-kanya-vivah',
    name: 'Savitribai Phule Divyang Kanya Vivah Protsahan Yojana',
    nativeNames: {
      en: 'Savitribai Phule Divyang Kanya Vivah Yojana (Marriage Grant)',
      hi: 'सावित्रीबाई फुले दिव्यांग कन्या विवाह प्रोत्साहन योजना',
      mr: 'सावित्रीबाई फुले दिव्यांग कन्या विवाह प्रोत्साहन योजना (अनुदान)',
    },
    shortDescription: {
      en: 'Maharashtra State Government one-time financial grant of ₹50,000 provided to support the marriage and rehabilitation of visually impaired and disabled women.',
      hi: 'महाराष्ट्र सरकार द्वारा दृष्टिबाधित व दिव्यांग महिलाओं के विवाह व पुनर्वास के लिए ₹50,000 का एकमुश्त आर्थिक अनुदान।',
      mr: 'महाराष्ट्र शासनातर्फे दृष्टिहीन व दिव्यांग मुलींच्या विवाहासाठी आणि पुनर्वासनासाठी ₹५०,००० चे एकरकमी अर्थसाहाय्य.',
    },
    authority: 'Commissioner of Persons with Disabilities, Government of Maharashtra',
    jurisdiction: 'State (Maharashtra)',
    category: 'maharashtra',
    eligibility: [
      'Bride or bridegroom must be a certified disabled person with 40%+ visual impairment.',
      'Both parties must be at or above legal marriage age (Bride 18+, Groom 21+).',
      'Residents of Maharashtra with registered marriage under the Special Marriage Act or Hindu Marriage Act.',
    ],
    benefits: [
      '₹50,000 financial grant transferred directly to the bride’s bank account to support household establishment and economic independence.',
    ],
    documentsRequired: [
      'Marriage Registration Certificate issued by Registrar of Marriages.',
      'Disability Certificate / UDID Card.',
      'Age proof and Aadhaar Card of both husband and wife.',
      'Bank Account Passbook of the bride.',
      'Maharashtra Domicile Certificate.',
    ],
    applicationOpeningDate: 'Within 1 year of marriage',
    applicationClosingDate: 'Continuous',
    officialUrl: 'https://sjsa.maharashtra.gov.in',
    applicationInstructions: [
      'Apply to the District Social Welfare Officer (Zilla Parishad) within one year from the date of marriage registration.',
      'Submit the joint application form along with marriage certificate and UDID proof.',
    ],
    lastVerified: 'October 2026 (Maharashtra Social Justice Dept)',
    isFeatured: false,
  },
];

export const INITIAL_SCHEME_NOTIFICATIONS: SchemeNotification[] = [
  {
    id: 'notif-1',
    schemeId: 'national-scholarship-pwd',
    schemeName: 'National Scholarships for Students with Disabilities',
    title: 'NSP Pre-Matric & Post-Matric Scholarship Portal is Open',
    message: 'Applications are currently open for 2026-27 on scholarships.gov.in. Includes monthly reader allowance of ₹2,000 for visually impaired students. Verified deadline is November 30, 2026.',
    date: '2026-10-18',
    isRead: false,
    type: 'opening',
    officialUrl: 'https://scholarships.gov.in',
  },
  {
    id: 'notif-2',
    schemeId: 'adip-scheme',
    schemeName: 'ADIP Free Smart Cane & DAISY Player Scheme',
    title: 'Free Assistive Device Assessment Camp Open',
    message: 'ALIMCO is distributing free Smart Canes, DAISY Digital Players, and Screen-Reader Laptops under the Central ADIP scheme. Apply online with your UDID card.',
    date: '2026-10-15',
    isRead: false,
    type: 'update',
    officialUrl: 'https://disabilityaffairs.gov.in',
  },
  {
    id: 'notif-3',
    schemeId: 'sanjay-gandhi-pension-maharashtra',
    schemeName: 'Sanjay Gandhi Niradhar Anudan Yojana (Divyang Pension)',
    title: 'Maharashtra Monthly Pension DBT Revision (₹1,500/Month)',
    message: 'Monthly financial assistance under Sanjay Gandhi Niradhar Anudan is disbursed via Direct Benefit Transfer. Ensure your bank account is seeded with Aadhaar and your UDID is updated.',
    date: '2026-10-10',
    isRead: false,
    type: 'new',
    officialUrl: 'https://sanjaygandhianudan.maharashtra.gov.in',
  },
  {
    id: 'notif-4',
    schemeId: 'msrtc-bus-concession-maharashtra',
    schemeName: 'MSRTC 100% Free Bus Travel Concession',
    title: 'MSRTC Smart Travel Card Active Verification',
    message: '100% free travel for blind persons in MSRTC buses across Maharashtra. You can renew or verify your Smart Card at any local MSRTC depot with your UDID card.',
    date: '2026-09-28',
    isRead: true,
    type: 'update',
    officialUrl: 'https://msrtc.maharashtra.gov.in',
  },
];

const NOTIFICATIONS_STORAGE_KEY = 'drishti_scheme_notifications_v1';

export function getSavedNotifications(): SchemeNotification[] {
  if (typeof window === 'undefined') return INITIAL_SCHEME_NOTIFICATIONS;
  try {
    const raw = localStorage.getItem(NOTIFICATIONS_STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(NOTIFICATIONS_STORAGE_KEY, JSON.stringify(INITIAL_SCHEME_NOTIFICATIONS));
      return INITIAL_SCHEME_NOTIFICATIONS;
    }
    return JSON.parse(raw);
  } catch (e) {
    return INITIAL_SCHEME_NOTIFICATIONS;
  }
}

export function saveNotifications(notifications: SchemeNotification[]): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(NOTIFICATIONS_STORAGE_KEY, JSON.stringify(notifications));
  } catch (e) {
    console.error('Failed to save scheme notifications:', e);
  }
}

export function markNotificationAsRead(id: string): SchemeNotification[] {
  const current = getSavedNotifications();
  const updated = current.map((item) => (item.id === id ? { ...item, isRead: true } : item));
  saveNotifications(updated);
  return updated;
}

export function markAllNotificationsAsRead(): SchemeNotification[] {
  const current = getSavedNotifications();
  const updated = current.map((item) => ({ ...item, isRead: true }));
  saveNotifications(updated);
  return updated;
}
