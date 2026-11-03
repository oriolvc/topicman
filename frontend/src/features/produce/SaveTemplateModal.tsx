import { Button, Group, Modal, Select, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import type { Broker } from '../../api/messaging';
import { ApiProblem } from '../../api/problem';
import { useAddTemplate, useCollections, useCreateCollection } from '../../shared/collections';
import { showProblem } from '../../shared/showProblem';
import { toTemplateRequest, type ProduceFormValues } from './produceForm';

/** The collection picker's value for "create a collection with the name below". */
const NEW_COLLECTION = 'new';

type SaveTemplateValues = {
  collection: string | null;
  newCollectionName: string;
  templateName: string;
};

type SaveTemplateModalProps = {
  values: ProduceFormValues;
  broker: Broker;
  onSaved: (collectionId: number, templateId: number) => void;
  onClose: () => void;
};

/** Saves the produce form as a new template, in an existing collection or a new one. */
export function SaveTemplateModal({ values, broker, onSaved, onClose }: SaveTemplateModalProps) {
  const collections = useCollections();
  const createCollection = useCreateCollection();
  const addTemplate = useAddTemplate();
  const existing = collections.data ?? [];
  const noCollections = collections.isSuccess && existing.length === 0;
  const form = useForm<SaveTemplateValues>({
    mode: 'controlled',
    initialValues: { collection: null, newCollectionName: '', templateName: '' },
    validate: (draft) => {
      const errors: Record<string, string> = {};
      const creating = noCollections || draft.collection === NEW_COLLECTION;
      if (!creating && draft.collection === null) {
        errors['collection'] = 'Choose a collection';
      }
      if (creating && draft.newCollectionName.trim() === '') {
        errors['newCollectionName'] = 'Enter a name';
      }
      if (draft.templateName.trim() === '') {
        errors['templateName'] = 'Enter a name';
      }
      return errors;
    },
  });
  const creating = noCollections || form.values.collection === NEW_COLLECTION;

  const save = async (draft: SaveTemplateValues) => {
    let collectionId: number;
    let collectionName: string;
    if (creating) {
      try {
        const created = await createCollection.mutateAsync(draft.newCollectionName.trim());
        collectionId = created.id;
        collectionName = created.name;
        // A failure below must not create the collection twice on the next try.
        form.setFieldValue('collection', String(created.id));
      } catch (error) {
        if (error instanceof ApiProblem && error.code === 'collection.name-taken') {
          form.setFieldError('newCollectionName', 'Another collection already has this name');
        } else {
          showProblem(error, 'The collection could not be created');
        }
        return;
      }
    } else {
      collectionId = Number(draft.collection);
      collectionName = existing.find(({ id }) => id === collectionId)?.name ?? '';
    }
    try {
      const template = await addTemplate.mutateAsync({
        collectionId,
        request: toTemplateRequest(values, draft.templateName.trim(), broker),
      });
      notifications.show({
        color: 'green',
        message: `Template ${template.name} saved to ${collectionName}`,
      });
      onSaved(collectionId, template.id);
    } catch (error) {
      if (error instanceof ApiProblem && error.code === 'collection.template-name-taken') {
        form.setFieldError('templateName', 'The collection already has a template with this name');
      } else {
        showProblem(error, 'The template could not be saved');
      }
    }
  };

  return (
    <Modal opened onClose={onClose} title="Save as template">
      <form onSubmit={form.onSubmit((draft) => void save(draft))} noValidate>
        <Stack>
          {!noCollections && (
            <Select
              label="Collection"
              withAsterisk
              placeholder={collections.isPending ? 'Loading collections…' : 'Choose one'}
              data={[
                ...existing.map(({ id, name }) => ({ value: String(id), label: name })),
                { value: NEW_COLLECTION, label: 'New collection…' },
              ]}
              {...form.getInputProps('collection')}
            />
          )}
          {creating && (
            <TextInput
              label="New collection name"
              withAsterisk
              {...form.getInputProps('newCollectionName')}
            />
          )}
          <TextInput
            label="Template name"
            withAsterisk
            description="Placeholders such as {{name}} are saved as written"
            {...form.getInputProps('templateName')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={createCollection.isPending || addTemplate.isPending}>
              Save
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
