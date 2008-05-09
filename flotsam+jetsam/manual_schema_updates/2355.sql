BEGIN;

INSERT INTO schema_history (screensaver_revision, date_updated, comment)
SELECT
2355,
current_timestamp,
'date type changes';

ALTER TABLE abase_testset RENAME testset_date TO testset_date_OLD;
ALTER TABLE administrative_activity RENAME date_approved TO date_approved_OLD;
ALTER TABLE activity RENAME date_of_activity TO date_of_activity_OLD;
ALTER TABLE billing_information RENAME billing_info_return_date TO billing_info_return_date_OLD;
ALTER TABLE billing_information RENAME date_charged TO date_charged_OLD;
ALTER TABLE billing_information RENAME date_completed5kcompounds TO date_completed5kcompounds_OLD;
ALTER TABLE billing_information RENAME date_faxed_to_billing_department TO date_faxed_to_billing_department_OLD;
ALTER TABLE billing_information RENAME fee_form_requested_date TO fee_form_requested_date_OLD;
ALTER TABLE billing_item RENAME date_faxed TO date_faxed_OLD;
ALTER TABLE checklist_item RENAME activation_date TO activation_date_OLD;
ALTER TABLE checklist_item RENAME deactivation_date TO deactivation_date_OLD;
ALTER TABLE cherry_pick_request RENAME date_requested TO date_requested_OLD;
ALTER TABLE cherry_pick_request RENAME date_volume_approved TO date_volume_approved_OLD;
ALTER TABLE copy_action RENAME date TO date_OLD;
ALTER TABLE copy_info RENAME date_plated TO date_plated_OLD;
ALTER TABLE copy_info RENAME date_retired TO date_retired_OLD;
ALTER TABLE letter_of_support RENAME date_written TO date_written_OLD;
ALTER TABLE library RENAME date_received TO date_received_OLD;
ALTER TABLE library RENAME date_screenable TO date_screenable_OLD;
ALTER TABLE screen RENAME data_meeting_complete TO data_meeting_complete_OLD;
ALTER TABLE screen RENAME data_meeting_scheduled TO data_meeting_scheduled_OLD;
ALTER TABLE screen RENAME date_of_application TO date_of_application_OLD;
ALTER TABLE screen RENAME publishable_protocol_date_entered TO publishable_protocol_date_entered_OLD;
ALTER TABLE screening RENAME assay_protocol_last_modified_date TO assay_protocol_last_modified_date_OLD;
ALTER TABLE screensaver_user RENAME harvard_id_expiration_date TO harvard_id_expiration_date_OLD;
ALTER TABLE status_item RENAME status_date TO status_date_OLD;

ALTER TABLE abase_testset ADD testset_date DATE;
ALTER TABLE administrative_activity ADD date_approved DATE;
ALTER TABLE activity ADD date_of_activity DATE;
ALTER TABLE billing_information ADD billing_info_return_date DATE;
ALTER TABLE billing_information ADD date_charged DATE;
ALTER TABLE billing_information ADD date_completed5kcompounds DATE;
ALTER TABLE billing_information ADD date_faxed_to_billing_department DATE;
ALTER TABLE billing_information ADD fee_form_requested_date DATE;
ALTER TABLE billing_item ADD date_faxed DATE;
ALTER TABLE checklist_item ADD activation_date DATE;
ALTER TABLE checklist_item ADD deactivation_date DATE;
ALTER TABLE cherry_pick_request ADD date_requested DATE;
ALTER TABLE cherry_pick_request ADD date_volume_approved DATE;
ALTER TABLE copy_action ADD date DATE;
ALTER TABLE copy_info ADD date_plated DATE;
ALTER TABLE copy_info ADD date_retired DATE;
ALTER TABLE letter_of_support ADD date_written DATE;
ALTER TABLE library ADD date_received DATE;
ALTER TABLE library ADD date_screenable DATE;
ALTER TABLE screen ADD data_meeting_complete DATE;
ALTER TABLE screen ADD data_meeting_scheduled DATE;
ALTER TABLE screen ADD date_of_application DATE;
ALTER TABLE screen ADD publishable_protocol_date_entered DATE;
ALTER TABLE screening ADD assay_protocol_last_modified_date DATE;
ALTER TABLE screensaver_user ADD harvard_id_expiration_date DATE;
ALTER TABLE status_item ADD status_date DATE;

UPDATE abase_testset SET testset_date = testset_date_OLD;
UPDATE administrative_activity SET date_approved = date_approved_OLD;
UPDATE activity SET date_of_activity = date_of_activity_OLD;
UPDATE billing_information SET billing_info_return_date = billing_info_return_date_OLD;
UPDATE billing_information SET date_charged = date_charged_OLD;
UPDATE billing_information SET date_completed5kcompounds = date_completed5kcompounds_OLD;
UPDATE billing_information SET date_faxed_to_billing_department = date_faxed_to_billing_department_OLD;
UPDATE billing_information SET fee_form_requested_date = fee_form_requested_date_OLD;
UPDATE billing_item SET date_faxed = date_faxed_OLD;
UPDATE checklist_item SET activation_date = activation_date_OLD;
UPDATE checklist_item SET deactivation_date = deactivation_date_OLD;
UPDATE cherry_pick_request SET date_requested = date_requested_OLD;
UPDATE cherry_pick_request SET date_volume_approved = date_volume_approved_OLD;
UPDATE copy_action SET date = date_OLD;
UPDATE copy_info SET date_plated = date_plated_OLD;
UPDATE copy_info SET date_retired = date_retired_OLD;
UPDATE letter_of_support SET date_written = date_written_OLD;
UPDATE library SET date_received = date_received_OLD;
UPDATE library SET date_screenable = date_screenable_OLD;
UPDATE screen SET data_meeting_complete = data_meeting_complete_OLD;
UPDATE screen SET data_meeting_scheduled = data_meeting_scheduled_OLD;
UPDATE screen SET date_of_application = date_of_application_OLD;
UPDATE screen SET publishable_protocol_date_entered = publishable_protocol_date_entered_OLD;
UPDATE screening SET assay_protocol_last_modified_date = assay_protocol_last_modified_date_OLD;
UPDATE screensaver_user SET harvard_id_expiration_date = harvard_id_expiration_date_OLD;
UPDATE status_item SET status_date = status_date_OLD;

ALTER TABLE abase_testset ALTER testset_date SET NOT NULL;
ALTER TABLE activity ALTER date_of_activity SET NOT NULL;
ALTER TABLE billing_item ALTER date_faxed SET NOT NULL;
ALTER TABLE cherry_pick_request ALTER date_requested SET NOT NULL;
ALTER TABLE copy_action ALTER date SET NOT NULL;
ALTER TABLE letter_of_support ALTER date_written SET NOT NULL;
ALTER TABLE status_item ALTER status_date SET NOT NULL;

ALTER TABLE abase_testset DROP testset_date_OLD;
ALTER TABLE administrative_activity DROP date_approved_OLD;
ALTER TABLE activity DROP date_of_activity_OLD;
ALTER TABLE billing_information DROP billing_info_return_date_OLD;
ALTER TABLE billing_information DROP date_charged_OLD;
ALTER TABLE billing_information DROP date_completed5kcompounds_OLD;
ALTER TABLE billing_information DROP date_faxed_to_billing_department_OLD;
ALTER TABLE billing_information DROP fee_form_requested_date_OLD;
ALTER TABLE billing_item DROP date_faxed_OLD;
ALTER TABLE checklist_item DROP activation_date_OLD;
ALTER TABLE checklist_item DROP deactivation_date_OLD;
ALTER TABLE cherry_pick_request DROP date_requested_OLD;
ALTER TABLE cherry_pick_request DROP date_volume_approved_OLD;
ALTER TABLE copy_action DROP date_OLD;
ALTER TABLE copy_info DROP date_plated_OLD;
ALTER TABLE copy_info DROP date_retired_OLD;
ALTER TABLE letter_of_support DROP date_written_OLD;
ALTER TABLE library DROP date_received_OLD;
ALTER TABLE library DROP date_screenable_OLD;
ALTER TABLE screen DROP data_meeting_complete_OLD;
ALTER TABLE screen DROP data_meeting_scheduled_OLD;
ALTER TABLE screen DROP date_of_application_OLD;
ALTER TABLE screen DROP publishable_protocol_date_entered_OLD;
ALTER TABLE screening DROP assay_protocol_last_modified_date_OLD;
ALTER TABLE screensaver_user DROP harvard_id_expiration_date_OLD;
ALTER TABLE status_item DROP status_date_OLD;

ALTER TABLE screen_result DROP date_created;
ALTER TABLE screen_result ADD date_created TIMESTAMP;
UPDATE screen_result SET date_created = date_last_imported;
ALTER TABLE screen_result ALTER date_created SET NOT NULL;
ALTER TABLE screen_result DROP date_last_imported;

COMMIT;
