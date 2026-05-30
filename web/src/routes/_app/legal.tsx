import { Link, createFileRoute } from '@tanstack/react-router'

import { seo } from '@/lib/seo'

export const Route = createFileRoute('/_app/legal')({
  head: () => ({
    meta: seo({ title: 'Legal — Checkpoint' }),
  }),
  component: LegalPage,
})

function LegalPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:py-24 space-y-20">
      {/* Header */}
      <div className="text-center space-y-6">
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
          Legal <span className="text-primary">Information</span>
        </h1>
        <p className="mx-auto max-w-2xl text-lg text-muted-foreground sm:text-xl leading-relaxed">
          Please review our Terms of Service and Privacy Policy carefully. By
          using Checkpoint, you agree to these terms.
        </p>

        {/* Table of Contents */}
        <nav className="flex justify-center gap-6 pt-4">
          <Link
            to="/legal"
            hash="terms"
            className="text-primary font-semibold hover:underline"
          >
            Terms of Service
          </Link>
          <span className="text-muted-foreground">|</span>
          <Link
            to="/legal"
            hash="privacy"
            className="text-primary font-semibold hover:underline"
          >
            Privacy Policy
          </Link>
        </nav>
      </div>

      {/* Terms of Service */}
      <section id="terms" className="scroll-mt-28 space-y-10">
        <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
          Terms of Service
        </h2>
        <p className="text-sm text-muted-foreground">
          Last updated: April 2026
        </p>

        <div className="space-y-8 text-muted-foreground leading-relaxed">
          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              1. Acceptance of Terms
            </h3>
            <p>
              By accessing or using Checkpoint ("the Service"), you agree to be
              bound by these Terms of Service. If you do not agree to these
              terms, please do not use the Service. We reserve the right to
              update these terms at any time, and continued use of the Service
              after changes constitutes acceptance of the revised terms.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              2. User Accounts and Responsibilities
            </h3>
            <p>
              To use certain features, you must create an account by providing
              accurate and complete information. You are responsible for
              maintaining the confidentiality of your account credentials and
              for all activities that occur under your account. You must be at
              least 16 years old to create an account. You agree to notify us
              immediately of any unauthorized use of your account.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              3. Content Ownership and Licensing
            </h3>
            <p>
              You retain ownership of all content you create on Checkpoint,
              including reviews, ratings, lists, and other user-generated
              content. By posting content, you grant Checkpoint a non-exclusive,
              worldwide, royalty-free license to display, distribute, and
              promote your content within the Service. Game metadata (titles,
              covers, descriptions) is sourced from third-party providers and
              remains the property of their respective owners.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              4. Prohibited Conduct
            </h3>
            <p>You agree not to:</p>
            <ul className="list-disc pl-6 space-y-1">
              <li>
                Use the Service for any unlawful purpose or in violation of any
                applicable laws
              </li>
              <li>
                Post content that is defamatory, abusive, hateful, or otherwise
                objectionable
              </li>
              <li>
                Attempt to gain unauthorized access to other users' accounts or
                the Service's infrastructure
              </li>
              <li>
                Use automated tools, bots, or scrapers to access the Service
                without prior written permission
              </li>
              <li>
                Interfere with or disrupt the integrity or performance of the
                Service
              </li>
            </ul>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              5. Termination and Account Deletion
            </h3>
            <p>
              You may delete your account at any time through your account
              settings. We reserve the right to suspend or terminate your
              account if you violate these terms. Upon termination, your right
              to use the Service ceases immediately. We may retain certain data
              as required by law or for legitimate business purposes, as
              described in our Privacy Policy.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              6. Limitation of Liability
            </h3>
            <p>
              The Service is provided "as is" and "as available" without
              warranties of any kind, either express or implied. Checkpoint
              shall not be liable for any indirect, incidental, special,
              consequential, or punitive damages arising out of or relating to
              your use of the Service. Our total liability shall not exceed the
              amount you have paid to Checkpoint, if any, in the twelve months
              preceding the claim.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              7. Governing Law
            </h3>
            <p>
              These Terms shall be governed by and construed in accordance with
              the laws of France. Any disputes arising from these terms or the
              use of the Service shall be subject to the exclusive jurisdiction
              of the courts of Strasbourg, France.
            </p>
          </div>
        </div>
      </section>

      {/* Divider */}
      <hr className="border-t border-border" />

      {/* Privacy Policy */}
      <section id="privacy" className="scroll-mt-28 space-y-10">
        <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
          Privacy Policy
        </h2>
        <p className="text-sm text-muted-foreground">
          Last updated: April 2026
        </p>

        <div className="space-y-8 text-muted-foreground leading-relaxed">
          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              1. Data We Collect
            </h3>
            <p>When you use Checkpoint, we may collect the following data:</p>
            <ul className="list-disc pl-6 space-y-1">
              <li>
                <strong>Account information:</strong> email address, username,
                and profile picture
              </li>
              <li>
                <strong>Library data:</strong> games in your collection, play
                status, play logs, and playtime
              </li>
              <li>
                <strong>User-generated content:</strong> reviews, ratings,
                lists, and tags
              </li>
              <li>
                <strong>Technical data:</strong> IP address, browser type, and
                device information for security and analytics
              </li>
            </ul>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              2. How We Use Your Data
            </h3>
            <p>We use your data to:</p>
            <ul className="list-disc pl-6 space-y-1">
              <li>Provide and maintain the Service</li>
              <li>
                Personalize your experience (e.g., recommendations, activity
                feeds)
              </li>
              <li>
                Enable social features such as friend connections and public
                profiles
              </li>
              <li>
                Communicate with you about your account or important Service
                updates
              </li>
              <li>
                Improve the Service through aggregated, anonymized analytics
              </li>
            </ul>
            <p>
              We do not sell your personal data to third parties. We do not use
              your data for advertising purposes.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              3. Cookies and Session Management
            </h3>
            <p>
              Checkpoint uses essential cookies to manage your authentication
              session and remember your preferences. We do not use third-party
              tracking cookies or advertising cookies. Session data is stored
              securely and expires automatically after a period of inactivity.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              4. Third-Party Services
            </h3>
            <p>
              We use the IGDB API (provided by Twitch/Amazon) to retrieve game
              metadata such as titles, descriptions, cover art, and release
              dates. No personal user data is shared with IGDB. Your game
              library and activity data remain exclusively on Checkpoint's
              servers.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              5. Data Retention and Deletion
            </h3>
            <p>
              We retain your data for as long as your account is active. When
              you delete your account, we will permanently delete your personal
              data within 30 days, except where retention is required by law.
              Backups containing your data may persist for up to 90 days before
              being purged.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              6. Your Rights (GDPR)
            </h3>
            <p>
              Under the General Data Protection Regulation (GDPR), as a resident
              of the European Union, you have the following rights:
            </p>
            <ul className="list-disc pl-6 space-y-1">
              <li>
                <strong>Right of access:</strong> request a copy of the personal
                data we hold about you
              </li>
              <li>
                <strong>Right to rectification:</strong> request correction of
                inaccurate or incomplete data
              </li>
              <li>
                <strong>Right to erasure:</strong> request deletion of your
                personal data
              </li>
              <li>
                <strong>Right to data portability:</strong> receive your data in
                a structured, commonly used format
              </li>
              <li>
                <strong>Right to object:</strong> object to the processing of
                your personal data
              </li>
              <li>
                <strong>Right to restrict processing:</strong> request
                limitation of how your data is processed
              </li>
            </ul>
            <p>
              To exercise any of these rights, please contact us at the address
              below. We will respond to your request within 30 days.
            </p>
          </div>

          <div className="space-y-3">
            <h3 className="text-xl font-semibold text-foreground">
              7. Contact for Privacy Inquiries
            </h3>
            <p>
              For any questions or concerns regarding your privacy or this
              policy, you can reach us at:
            </p>
            <p>
              <a
                href="mailto:privacy@checkpoint.app"
                className="text-primary hover:underline"
              >
                privacy@checkpoint.app
              </a>
            </p>
            <p>
              Checkpoint
              <br />
              Strasbourg, France
            </p>
          </div>
        </div>
      </section>
    </div>
  )
}
